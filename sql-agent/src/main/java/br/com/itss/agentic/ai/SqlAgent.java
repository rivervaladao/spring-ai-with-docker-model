package br.com.itss.agentic.ai;

import br.com.itss.agentic.ai.Models.*;
import br.com.itss.agentic.ai.tools.DbTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SQL Agent com structured-output robusto e pré-renderização dos prompts:
 *  - Renderiza o prompt-base (com <placeholders>) usando TemplateRenderer
 *  - Depois ANEXA o schema getFormat() "cru" (sem passar pelo parser)
 *  - Converte manualmente as respostas com BeanOutputConverter + fallbacks
 */
@Service
public class SqlAgent {

    private static final Logger log = LoggerFactory.getLogger(SqlAgent.class);

    private final ChatClient chat;
    private final DbTools db;
    private final TemplateRenderer renderer;
    private final ObjectMapper om = new ObjectMapper();

    public SqlAgent(ChatClient.Builder builder, DbTools dbTools, TemplateRenderer templateRenderer) {
        this.chat = builder
                .defaultTemplateRenderer(templateRenderer) // garante <...> neste ChatClient
                .defaultTools(dbTools)                     // expõe tools do DB, se quiser usar futuramente
                .build();
        this.db = dbTools;
        this.renderer = templateRenderer;
    }

    public FinalAnswer answer(String question, int maxRetries) {
        log.info("▶️ SQL Agent start | question='{}'", question);

        // (1) listar tabelas
        List<String> allTables = db.listTables();

        // (2) escolher relevantes (structured-output)
        var selectConv = new BeanOutputConverter<>(TableSelection.class);
        final String selectFormat = selectConv.getFormat();

        // pré-filtro opcional para prompts muito grandes
        List<String> tables = prefilterTables(allTables, question, 200);
        if (tables.size() < allTables.size()) {
            log.info("🔎 Prefilter: using {} of {} tables to keep prompt short", tables.size(), allTables.size());
        }
        final String tablesStr = String.join(", ", tables);

        // PRÉ-RENDER: preenche <question> e <tables> AGORA
        String selectBase = renderer.apply(
                Prompts.TABLE_SELECTOR_PROMPT,
                Map.of("question", question, "tables", tablesStr)
        );
        // depois ANEXA o schema cru (sem templating)
        String selectPrompt = selectBase + "\n\nSiga EXATAMENTE este formato:\n" + selectFormat;

        // envia como TEXTO pronto (sem .param -> evita novo parse de template)
        var selectRaw = chat.prompt()
                .user(selectPrompt)
                .call()
                .content();

        log.debug("RAW TableSelection: {}", preview(selectRaw, 300));

        TableSelection selection = safeConvert(selectConv, selectRaw, TableSelection.class);
        if (selection == null || selection.tables() == null || selection.tables().isEmpty()) {
            log.error("❌ TableSelection vazio/inválido.\nPROMPT:\n{}\n\nRAW:\n{}", selectPrompt, selectRaw);
            return new FinalAnswer("Não encontrei tabelas relevantes para a pergunta.", "");
        }

        List<String> relevant = selection.tables();
        log.info("✅ Relevant tables: {} | reason='{}'", relevant, selection.reason());

        // (3) schemas relevantes
        List<String> schemas = new ArrayList<>();
        for (String t : relevant) {
            schemas.add(db.getTableSchema(t));
        }
        String schemasBlob = String.join("\n\n", schemas);
        final String schemasStr = schemasBlob;

        // (4) gerar SQL (structured-output)
        var draftConv = new BeanOutputConverter<>(SqlDraft.class);
        final String draftFormat = draftConv.getFormat();

        String draftBase = renderer.apply(
                Prompts.SQL_GENERATOR_PROMPT,
                Map.of("question", question, "schemas", schemasStr)
        );
        String draftPrompt = draftBase + "\n\nSiga EXATAMENTE este formato:\n" + draftFormat;

        var draftRaw = chat.prompt()
                .user(draftPrompt)
                .call()
                .content();

        log.debug("RAW SqlDraft: {}", preview(draftRaw, 300));

        SqlDraft draft = safeConvert(draftConv, draftRaw, SqlDraft.class);
        if (draft == null || draft.sql() == null || draft.sql().isBlank()) {
            log.error("❌ SqlDraft inválido.\nPROMPT:\n{}\n\nRAW:\n{}", draftPrompt, draftRaw);
            return new FinalAnswer("Não foi possível gerar uma consulta SQL.", "");
        }

        String sql = draft.sql();
        log.info("📝 SQL draft: {}", sql);

        // (5) revisar SQL (structured-output)
        var reviewConv = new BeanOutputConverter<>(SqlReview.class);
        final String reviewFormat = reviewConv.getFormat();

        String reviewBase = renderer.apply(
                Prompts.SQL_REVIEW_PROMPT,
                Map.of("sql", sql)
        );
        String reviewPrompt = reviewBase + "\n\nSiga EXATAMENTE este formato:\n" + reviewFormat;

        var reviewRaw = chat.prompt()
                .user(reviewPrompt)
                .call()
                .content();

        log.debug("RAW SqlReview: {}", preview(reviewRaw, 300));

        SqlReview review = safeConvert(reviewConv, reviewRaw, SqlReview.class);
        if (review != null && !review.ok() && review.fixed_sql() != null && !review.fixed_sql().isBlank()) {
            log.info("🩺 Review suggested fix: issues='{}'", review.issues());
            sql = review.fixed_sql();
        }

        // (6/7) executar + corrigir em loop
        int attempts = 0;
        List<Map<String,Object>> rows;
        while (true) {
            attempts++;
            try {
                rows = db.executeSql(sql);
                break;
            } catch (Exception ex) {
                if (attempts > Math.max(1, maxRetries)) {
                    log.error("❌ SQL execution failed after {} attempts. Last error: {}", attempts-1, ex.toString());
                    return new FinalAnswer("Falha ao executar a consulta: " + ex.getMessage(), sql);
                }
                log.warn("⚠️ DB error on attempt {}: {} | asking LLM to fix...", attempts, ex.getMessage());

                var fixConv = new BeanOutputConverter<>(SqlErrorFix.class);
                final String fixFormat = fixConv.getFormat();

                String fixBase = renderer.apply(
                        Prompts.SQL_ERROR_FIX_PROMPT,
                        Map.of("sql", sql, "db_error", ex.getMessage(), "schemas", schemasStr)
                );
                String fixPrompt = fixBase + "\n\nSiga EXATAMENTE este formato:\n" + fixFormat;

                var fixRaw = chat.prompt()
                        .user(fixPrompt)
                        .call()
                        .content();

                log.debug("RAW SqlErrorFix: {}", preview(fixRaw, 400));

                SqlErrorFix fix = safeConvert(fixConv, fixRaw, SqlErrorFix.class);
                if (fix == null || fix.sql() == null || fix.sql().isBlank()) {
                    log.warn("LLM did not provide a fixed SQL. Stopping. RAW:\n{}", fixRaw);
                    return new FinalAnswer("Não foi possível corrigir a consulta automaticamente.", sql);
                }
                log.info("🔧 Fixed SQL (attempt {}): {}", attempts, fix.sql());
                sql = fix.sql();
            }
        }

        // (8) resposta final (structured-output opcional)
        String tableMd = formatRowsAsMarkdown(rows, 20);

        var answerConv = new BeanOutputConverter<>(FinalAnswer.class);
        final String answerFormat = answerConv.getFormat();

        String answerBase = renderer.apply(
                Prompts.FINAL_ANSWER_PROMPT,
                Map.of("question", question, "sql", sql, "result_table", tableMd)
        );
        String answerPrompt = answerBase + "\n\nSiga EXATAMENTE este formato:\n" + answerFormat;

        var answerRaw = chat.prompt()
                .user(answerPrompt)
                .call()
                .content();

        log.debug("RAW FinalAnswer: {}", preview(answerRaw, 400));

        FinalAnswer answer = safeConvert(answerConv, answerRaw, FinalAnswer.class);
        if (answer == null || answer.answer() == null) {
            log.warn("⚠️ FinalAnswer inválido. Devolvendo texto bruto.");
            return new FinalAnswer(answerRaw, sql);
        }

        log.info("✅ Done. rows={} | sql='{}'", rows.size(), sql);
        return new FinalAnswer(answer.answer(), sql);
    }

    // ----------------- helpers -----------------

    private static String preview(String s, int max) {
        if (s == null) return "";
        var t = s.strip();
        return t.length() <= max ? t : t.substring(0, max) + " ...";
    }

    /** Converte usando BeanOutputConverter com fallbacks leves. */
    private <T> T safeConvert(BeanOutputConverter<T> conv, String raw, Class<T> type) {
        try {
            if (raw == null || raw.isBlank()) return null;
            try {
                return conv.convert(raw);
            } catch (Exception e1) {
                // tenta sanitizar: pega o primeiro {...} do texto
                int i = raw.indexOf('{');
                int j = raw.lastIndexOf('}');
                if (i >= 0 && j > i) {
                    String json = raw.substring(i, j + 1);
                    return conv.convert(json);
                }
                // última tentativa: remover cercas ```json
                String cleaned = raw.replaceAll("(?s)```json\\s*", "")
                        .replaceAll("(?s)```", "")
                        .trim();
                if (!cleaned.equals(raw)) {
                    return conv.convert(cleaned);
                }
                throw e1;
            }
        } catch (Exception e) {
            log.error("safeConvert failure for {}: {}", type.getSimpleName(), e.toString());
            return null;
        }
    }

    /** Pré-filtro simples por termos da pergunta; reduz lista de tabelas a N. */
    private List<String> prefilterTables(List<String> all, String question, int max) {
        if (all.size() <= max) return all;

        var q = normalize(question);
        var terms = Arrays.stream(q.split("\\W+"))
                .filter(s -> s.length() >= 3)
                .collect(Collectors.toSet());

        return all.stream()
                .sorted((a, b) -> Integer.compare(
                        relevance(normalize(b), terms),
                        relevance(normalize(a), terms)))
                .limit(max)
                .toList();
    }

    private static String normalize(String s) {
        var t = Objects.toString(s, "").toLowerCase(Locale.ROOT);
        t = t.replace("ç", "c")
                .replace("ã", "a").replace("á", "a").replace("à", "a").replace("â", "a")
                .replace("é", "e").replace("ê", "e")
                .replace("í", "i")
                .replace("ó", "o").replace("ô", "o")
                .replace("ú", "u");
        return t;
    }

    private static int relevance(String table, Set<String> terms) {
        int score = 0;
        for (String term : terms) {
            if (table.contains(term)) score += 2;
            if (table.startsWith(term)) score += 1; // bônus por prefixo
        }
        return score;
    }

    private static String formatRowsAsMarkdown(List<Map<String,Object>> rows, int maxRows) {
        if (rows == null || rows.isEmpty()) return "_(sem linhas)_";
        List<Map<String,Object>> limited = rows.stream().limit(maxRows).toList();

        List<String> cols = new ArrayList<>(limited.get(0).keySet());
        String header = "| " + String.join(" | ", cols) + " |";
        String sep = "| " + cols.stream().map(c -> "---").collect(Collectors.joining(" | ")) + " |";
        List<String> body = new ArrayList<>();
        for (var r : limited) {
            List<String> vals = cols.stream().map(c -> Objects.toString(r.get(c), "")).toList();
            body.add("| " + String.join(" | ", vals) + " |");
        }
        return header + "\n" + sep + "\n" + String.join("\n", body);
    }
}
