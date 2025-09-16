package br.com.itss.agent.ai;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.chat.client.ChatClient;

import br.com.itss.agent.ai.ParallelizationWorkflow;

@SpringBootApplication
public class Main {

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

        final String ANALYSIS_PROMPT = """
                        Analise como as mudanças de mercado irão impactar este grupo de stakeholders.
                        Forneça impactos específicos e ações recomendadas.
                        Formate com seções claras e prioridades.
                        """;

        final List<String> STAKEHOLDERS = List.of(
                        """
                                        Clientes:
                                        - Sensíveis a preço
                                        - Desejam melhor tecnologia
                                        - Preocupações ambientais
                                        """,

                        """
                                        Colaboradores:
                                        - Preocupações com segurança no emprego
                                        - Necessidade de novas habilidades
                                        - Desejo de direção clara
                                        """,

                        """
                                        Investidores:
                                        - Expectativa de crescimento
                                        - Desejam controle de custos
                                        - Preocupações com riscos
                                        """,

                        """
                                        Fornecedores:
                                        - Restrições de capacidade
                                        - Pressões de preço
                                        - Transições tecnológicas
                                        """
        );
        
        int nWorkers = 4;

        @Bean
        public CommandLineRunner commandLineRunner(ChatClient.Builder chatClientBuilder) {

                return args -> {
                        // ------------------------------------------------------------
                        // FLUXO PARALELO
                        // -----------------------------------------------------------
                        List<String> parallelResponse = new ParallelizationWorkflow(chatClientBuilder.build())
                                        .parallel(ANALYSIS_PROMPT, STAKEHOLDERS, nWorkers);
                        System.out.println(parallelResponse);

                };
        }
}