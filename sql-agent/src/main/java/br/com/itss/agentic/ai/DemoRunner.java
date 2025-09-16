package br.com.itss.agentic.ai;

import br.com.itss.agentic.ai.Models.FinalAnswer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoRunner.class);

    @Bean
    CommandLineRunner runSqlAgent(SqlAgent agent, ChatClient.Builder chatClientBuilder) {
        return args -> {
            String question = """
          Liste as top 5 ordens de servico?
          """;

            FinalAnswer res = agent.answer(question, 3);

            log.info("\n\n===== RESPOSTA =====\n{}\n\n===== SQL =====\n{}\n", res.answer(), res.sql());
            System.out.println("\n\n===== RESPOSTA =====\n" + res.answer());
            System.out.println("\n===== SQL =====\n" + res.sql());
        };
    }
}
