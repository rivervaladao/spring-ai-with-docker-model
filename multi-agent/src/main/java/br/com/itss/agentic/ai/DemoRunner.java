package br.com.itss.agentic.ai;
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
    CommandLineRunner multiAgentRunner(MultiAgentService multiAgentService, ChatClient.Builder chatClientBuilder) {
        return args -> {
            String question = """
          java streams
          """;
            var res = multiAgentService.executeMultiAgent(question);

            log.info("\n\n===== PESQUISA =====\n{}\n", res.research());
            System.out.println("\n\n===== PESQUISA =====\n" + res.research());

            log.info("\n\n===== RESUMO =====\n{}\n", res.summary());
            System.out.println("\n\n===== RESUMO =====\n" + res.summary());
        };
    }
}