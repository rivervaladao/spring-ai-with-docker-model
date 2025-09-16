package br.com.itss.agent.ai;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.chat.client.ChatClient;

import br.com.itss.agent.ai.ChainWorkflow;

@SpringBootApplication
public class Main {

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

	String report = """
        Resumo de Desempenho do 3º Trimestre:
        Nossa pontuação de satisfação do cliente subiu para 92 pontos neste trimestre.
        A receita cresceu 45% em comparação com o ano passado.
        A participação de mercado está agora em 23% em nosso mercado principal.
        A taxa de churn de clientes diminuiu para 5%, vinda de 8%.
        O custo de aquisição de novos usuários é de US$43 por usuário.
        A taxa de adoção do produto aumentou para 78%.
        A satisfação dos colaboradores está em 87 pontos.
        A margem operacional melhorou para 34%.
        """;

	@Bean
	public CommandLineRunner commandLineRunner(ChatClient.Builder chatClientBuilder) {
		return args -> {
			new ChainWorkflow(chatClientBuilder.build()).chain(report);
		};

	}
}
