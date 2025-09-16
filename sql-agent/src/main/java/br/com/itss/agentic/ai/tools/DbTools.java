package br.com.itss.agentic.ai.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class DbTools {

    private static final Logger log = LoggerFactory.getLogger(DbTools.class);
    private final JdbcTemplate jdbc;

    public DbTools(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Tool(
            name = "list_tables",
            description = "Lista tabelas públicas disponíveis no banco (schema 'public')."
    )
    public List<String> listTables() {
        long t0 = System.currentTimeMillis();
        var sql = """
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema='public' AND table_type='BASE TABLE'
        ORDER BY table_name
        """;
        // aqui é 1 coluna => ok usar String.class
        var list = jdbc.queryForList(sql, String.class);
        log.info("[TOOL] list_tables -> {} tables ({} ms)", list.size(), (System.currentTimeMillis()-t0));
        return list;
    }

    @Tool(
            name = "get_table_schema",
            description = "Retorna o schema de uma tabela (coluna:tipo) e a(s) chave(s) primária(s)."
    )
    public String getTableSchema(String table) {
        long t0 = System.currentTimeMillis();

        // 1) Colunas e tipos (N colunas => use Map!)
        List<Map<String,Object>> cols = jdbc.queryForList("""
        SELECT column_name, data_type
        FROM information_schema.columns
        WHERE table_schema='public' AND table_name=?
        ORDER BY ordinal_position
        """, table);

        // 2) Colunas de PK via information_schema (evita regclass/qualificação)
        List<Map<String,Object>> pkRows = jdbc.queryForList("""
        SELECT kcu.column_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
         AND tc.table_schema = kcu.table_schema
        WHERE tc.constraint_type = 'PRIMARY KEY'
          AND tc.table_schema = 'public'
          AND tc.table_name = ?
        ORDER BY kcu.ordinal_position
        """, table);

        Set<String> pk = pkRows.stream()
                .map(m -> Objects.toString(m.get("column_name"), ""))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        String schema = cols.stream()
                .map(m -> {
                    String c = Objects.toString(m.get("column_name"), "");
                    String t = Objects.toString(m.get("data_type"), "");
                    return c + " " + t + (pk.contains(c) ? " PRIMARY KEY" : "");
                })
                .collect(Collectors.joining("\n"));

        String out = "TABLE " + table + "\n" + schema;
        log.info("[TOOL] get_table_schema('{}') -> {} cols, pk={} ({} ms)",
                table, cols.size(), pk, (System.currentTimeMillis()-t0));
        return out;
    }

    private static final Pattern SAFE_SELECT =
            Pattern.compile("^\\s*select\\b.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static String enforceLimit(String sql) {
        if (sql.toLowerCase().contains(" limit ")) return sql;
        return sql.trim() + " LIMIT 100";
    }

    @Tool(
            name = "execute_sql",
            description = "Executa UMA consulta SELECT (read-only). Adiciona LIMIT 100 se ausente."
    )
    public List<Map<String,Object>> executeSql(String sql) {
        long t0 = System.currentTimeMillis();
        if (sql == null || !SAFE_SELECT.matcher(sql).matches() || sql.contains(";")) {
            throw new IllegalArgumentException("Somente uma instrução SELECT simples é permitida.");
        }
        String finalSql = enforceLimit(sql);
        log.info("[TOOL] execute_sql START sql='{}'", finalSql);
        var rows = jdbc.queryForList(finalSql);
        log.info("[TOOL] execute_sql END rows={} ({} ms)", rows.size(), (System.currentTimeMillis()-t0));
        return rows;
    }
}
