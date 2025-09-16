package br.com.itss.agentic.ai;

import java.util.List;
interface Models {

    public record Decision(String next, String reason) {
    }               // p/ roteamento

    public record WorkerOutput(String role, String content) {
    }

    public record TeamState(
            String userGoal,
            List<String> notes,
            List<String> citations,
            List<String> charts,
            String draft
    ) {
    }
}