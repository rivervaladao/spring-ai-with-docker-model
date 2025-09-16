package br.com.itss.agentic.ai;

public interface Models {
    // POJO para Structured Output
    public record AgentResponse(String research, String summary) {}
}
