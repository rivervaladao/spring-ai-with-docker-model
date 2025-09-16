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
                RefinedResponse refinedResponse = new EvaluatorOptimizer(chatClient).loop("""
                        <user input>
                        Implemente uma pilha (Stack) em Java com:
                        1. push(x)
                        2. pop()
                        3. getMin()
                        Todas as operações devem ser O(1).
                        Todos os campos internos devem ser private e, quando utilizados, devem ser prefixados com 'this.'.
                        </user input>
                        """);

                System.out.println("SAÍDA FINAL:\n : " + refinedResponse);
        };
        }

        
}