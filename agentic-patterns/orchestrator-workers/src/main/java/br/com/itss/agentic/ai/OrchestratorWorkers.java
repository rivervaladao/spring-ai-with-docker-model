package br.com.itss.agent.ai;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.Assert;

public class OrchestratorWorkers {

	private final ChatClient chatClient;
	private final String orchestratorPrompt;
	private final String workerPrompt;

	public static final String DEFAULT_ORCHESTRATOR_PROMPT = """
			Analise esta tarefa e divida-a em 2-3 abordagens distintas:

			Tarefa: {task}

			Devolva sua resposta neste formato JSON:
			\\{
			"analysis": "Explique seu entendimento da tarefa e quais variações seriam valiosas.
			             Foque em como cada abordagem atende a diferentes aspectos da tarefa.",
			"tasks": [
				\\{
				"type": "formal",
				"description": "Escreva uma versão precisa e técnica que enfatize especificações"
				\\},
				\\{
				"type": "conversational",
				"description": "Escreva uma versão envolvente e amigável que conecte com os leitores"
				\\}
			]
			\\}
			""";

	public static final String DEFAULT_WORKER_PROMPT = """
			Gere conteúdo com base em:
			Tarefa: {original_task}
			Estilo: {task_type}
			Diretrizes: {task_description}
			""";

	public static record Task(String type, String description) {
	}

	public static record OrchestratorResponse(String analysis, List<Task> tasks) {
	}

	public static record FinalResponse(String analysis, List<String> workerResponses) {
	}

	public OrchestratorWorkers(ChatClient chatClient) {
		this(chatClient, DEFAULT_ORCHESTRATOR_PROMPT, DEFAULT_WORKER_PROMPT);
	}

	public OrchestratorWorkers(ChatClient chatClient, String orchestratorPrompt, String workerPrompt) {
		Assert.notNull(chatClient, "ChatClient não deve ser nulo");
		Assert.hasText(orchestratorPrompt, "Prompt do orquestrador não deve ser vazio");
		Assert.hasText(workerPrompt, "Prompt do trabalhador não deve ser vazio");

		this.chatClient = chatClient;
		this.orchestratorPrompt = orchestratorPrompt;
		this.workerPrompt = workerPrompt;
	}

	@SuppressWarnings("null")
	public FinalResponse process(String taskDescription) {
		Assert.hasText(taskDescription, "A descrição da tarefa não deve estar vazia");

		OrchestratorResponse orchestratorResponse = this.chatClient.prompt()
				.user(u -> u.text(this.orchestratorPrompt)
						.param("task", taskDescription))
				.call()
				.entity(OrchestratorResponse.class);

		System.out.println(String.format("\n=== SAÍDA DO ORQUESTRADOR ===\nANÁLISE: %s\n\nTAREFAS: %s\n",
				orchestratorResponse.analysis(), orchestratorResponse.tasks()));

		List<String> workerResponses = orchestratorResponse.tasks().stream().map(task -> this.chatClient.prompt()
				.user(u -> u.text(this.workerPrompt)
						.param("original_task", taskDescription)
						.param("task_type", task.type())
						.param("task_description", task.description()))
				.call()
				.content()).toList();

		System.out.println("\n=== SAIDA DO WORKER ===\n" + workerResponses);

		return new FinalResponse(orchestratorResponse.analysis(), workerResponses);
	}

}
