package br.com.itss.agent.ai;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.chat.client.ChatClient;

import br.com.itss.agent.ai.EvaluatorOptimizer;

@SpringBootApplication
public class Main {

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

        @Bean
        public CommandLineRunner commandLineRunner(ChatClient.Builder chatClientBuilder) {
                var chatClient = chatClientBuilder.build();
                return args -> {

                        new OrchestratorWorkers(chatClient)
                                .process("Escreva uma descrição de produto para uma nova garrafa de água ecológica");

                };
        }

        
}