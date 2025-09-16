package br.com.itss.agentic.ai.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Ferramentas para notas e rascunho de documento.
 */
@Component
public class DocumentTools {

    private static final Logger log = LoggerFactory.getLogger(DocumentTools.class);

    @Tool(
            name = "make_notes",
            description = """
            Converte texto em notas enxutas (bullets) por sentença.
            Parâmetros:
            - content (obrigatório)
            """
    )
    public String makeNotes(String content) {
        long t0 = System.currentTimeMillis();
        int inLen = content == null ? 0 : content.length();
        log.info("[TOOL] make_notes START contentLen={}", inLen);

        if (content == null || content.isBlank()) {
            log.info("[TOOL] make_notes END empty content elapsedMs={}", (System.currentTimeMillis() - t0));
            return "## Notas\n(nenhum conteúdo)";
        }

        var bullets = Arrays.stream(content.split("(?<=[.!?])\\s+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> "- " + s)
                .collect(Collectors.joining("\n"));

        var out = "## Notas\n" + bullets;

        log.info("[TOOL] make_notes END outLen={} elapsedMs={}", out.length(), (System.currentTimeMillis() - t0));
        return out;
    }

    @Tool(
            name = "write_draft",
            description = """
            Gera um rascunho de documento Markdown a partir de meta e notas.
            Parâmetros:
            - goal (obrigatório)
            - notes (obrigatório)
            """
    )
    public String writeDraft(String goal, String notes) {
        long t0 = System.currentTimeMillis();
        log.info("[TOOL] write_draft START goalPreview='{}' notesLen={}",
                preview(goal, 60), notes == null ? 0 : notes.length());

        String safeGoal = (goal == null || goal.isBlank()) ? "(meta não informada)" : goal;
        String safeNotes = (notes == null || notes.isBlank()) ? "(sem notas)" : notes;

        String out = """
               # Relatório: %s

               ## Resumo Executivo
               Documento inicial elaborado a partir de notas e pesquisa (rascunho).

               ## Notas Consolidadas
               %s

               ## Conclusão
               Próximos passos: validar fontes, enriquecer dados e revisar narrativa.
               """.formatted(safeGoal, safeNotes);

        log.info("[TOOL] write_draft END outLen={} elapsedMs={}", out.length(), (System.currentTimeMillis() - t0));
        return out;
    }

    private static String preview(String s, int max) {
        if (s == null) return "";
        var t = s.strip();
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }
}
