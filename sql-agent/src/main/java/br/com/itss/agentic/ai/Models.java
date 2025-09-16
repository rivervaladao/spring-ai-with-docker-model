package br.com.itss.agentic.ai;

import java.util.List;
import java.util.Map;

interface Models {
    public record TableSelection(List<String> tables, String reason) {}
    public record SqlDraft(String sql, String thoughts) {}
    public record SqlReview(boolean ok, String issues, String fixed_sql) {}
    public record SqlErrorFix(String sql, String reason) {}
    public record QueryResult(List<String> columns, List<Map<String,Object>> rows) {}
    public record FinalAnswer(String answer, String sql) {}
}