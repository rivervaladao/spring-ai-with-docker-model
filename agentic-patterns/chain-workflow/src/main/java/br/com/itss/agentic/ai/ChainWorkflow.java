package br.com.itss.agent.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class ChainWorkflow {
	private static final String[] DEFAULT_SYSTEM_PROMPTS = {

			// Etapa 1
			"""
					Extraia apenas os valores numéricos e suas métricas associadas do texto.
					Formate cada um como 'valor: métrica' em uma nova linha.
					Exemplo de formato:
					92: satisfação do cliente
					45%: crescimento da receita""",
			// Etapa 2
			"""
					Converta todos os valores numéricos em porcentagens sempre que possível.
					Se não for uma porcentagem ou pontos, converta para percentual (ex.: 92 pontos -> 92%).
					Mantenha um número por linha.
					Exemplo de formato:
					92%: satisfação do cliente
					45%: crescimento da receita""",
			// Etapa 3
			"""
					Ordene todas as linhas em ordem decrescente pelo valor numérico.
					Mantenha o formato 'valor: métrica' em cada linha.
					Exemplo:
					92%: satisfação do cliente
					87%: satisfação dos colaboradores""",
			// Etapa 4
			"""
					Formate os dados ordenados como uma tabela markdown com as colunas:
					| Métrica | Valor |
					|:--|--:|
					| Satisfação do Cliente | 92% | """
	};

	private final ChatClient chatClient;

	private final String[] systemPrompts;

	public ChainWorkflow(ChatClient chatClient) {
		this(chatClient, DEFAULT_SYSTEM_PROMPTS);
	}

	public ChainWorkflow(ChatClient chatClient, String[] systemPrompts) {
		this.chatClient = chatClient;
		this.systemPrompts = systemPrompts;
	}

	public String chain(String userInput) {

		int step = 0;
		String response = userInput;
		System.out.println(String.format("\nETAPA %s:\n %s", step++, response));

		for (String prompt : systemPrompts) {

			// 1. Compose the input using the response from the previous step.
			String input = String.format("{%s}\n {%s}", prompt, response);

			// 2. Call the chat client with the new input and get the new response.
			response = chatClient.prompt(input).call().content();

			System.out.println(String.format("\nETAPA %s:\n %s", step++, response));
		}

		return response;
	}
}