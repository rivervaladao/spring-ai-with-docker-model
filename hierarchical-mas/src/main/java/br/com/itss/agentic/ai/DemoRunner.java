package br.com.itss.agentic.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoRunner {

    @Bean
    CommandLineRunner hierarchicalAgentsDemo(ChatClient.Builder chatClientBuilder) {
        return args -> {
            var chat = chatClientBuilder.build();
            var team = new HierarchicalTeam(chat);

            var goal = """
                    tendencias sobre desenvolvimento de IA com java.
                    """;

            var res = team.run(goal, 8);

            System.out.println("\n=== DRAFT (Documento) ===\n" + res.draft());
            System.out.println("\n=== NOTES (Notas agregadas) ===\n" + String.join("\n\n---\n\n", res.notes()));
            System.out.println("\n=== CHARTS (Propostas de gráficos) ===\n" + String.join("\n\n---\n\n", res.charts()));
            System.out.println("\n=== TURNS === " + res.turns());
        };
    }
}
