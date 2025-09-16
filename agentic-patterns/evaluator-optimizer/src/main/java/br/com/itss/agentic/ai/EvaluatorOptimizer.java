package br.com.itss.agent.ai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.Assert;

@SuppressWarnings("null")
public class EvaluatorOptimizer {

	public static final String DEFAULT_GENERATOR_PROMPT = """
			Seu objetivo é completar a tarefa com base na entrada. 
			Se houver feedback de gerações anteriores, você deve refletir sobre eles para melhorar sua solução.

			CRÍTICO: Sua resposta deve ser uma ÚNICA LINHA de JSON válido, SEM QUEBRAS DE LINHA exceto as explicitamente escapadas com \\n.
			Aqui está o formato exato a ser seguido, incluindo todas as aspas e chaves:

			{"thoughts":"Breve descrição aqui","response":"public class Example {\\n    // Código aqui\\n}"}

			Regras para o campo response:
			1. TODAS as quebras de linha devem usar \\n
			2. TODAS as aspas devem usar \\"
			3. TODAS as barras invertidas devem ser duplicadas: \\
			4. NENHUMA quebra de linha real ou formatação – tudo em uma única linha
			5. NENHUM tab ou caractere especial
			6. O código Java deve ser completo e devidamente escapado

			Exemplo de resposta corretamente formatada:
			{"thoughts":"Implementando contador","response":"public class Counter {\\n    private int count;\\n    public Counter() {\\n        count = 0;\\n    }\\n    public void increment() {\\n        count++;\\n    }\\n}"}

			Siga esse formato EXATAMENTE – sua resposta deve ser um JSON válido em uma única linha.
			""";

	public static final String DEFAULT_EVALUATOR_PROMPT = """
			Avalie esta implementação de código quanto à correção, complexidade de tempo e boas práticas.
			Garanta que o código tenha documentação javadoc adequada.
			Responda com EXATAMENTE este formato JSON em uma única linha:

			{"evaluation":"PASS, NEEDS_IMPROVEMENT, or FAIL", "feedback":"Seu feedback aqui"}

			O campo evaluation deve ser um destes: "PASS", "NEEDS_IMPROVEMENT", "FAIL"
			Use "PASS" apenas se todos os critérios forem atendidos sem necessidade de melhorias.
			""";

	public static record Generation(String thoughts, String response) {
	}

	public static record EvaluationResponse(Evaluation evaluation, String feedback) {
		public enum Evaluation {
			PASS, NEEDS_IMPROVEMENT, FAIL
		}
	}

	public static record RefinedResponse(String solution, List<Generation> chainOfThought) {
	}

	private final ChatClient chatClient;
	private final String generatorPrompt;
	private final String evaluatorPrompt;

	public EvaluatorOptimizer(ChatClient chatClient) {
		this(chatClient, DEFAULT_GENERATOR_PROMPT, DEFAULT_EVALUATOR_PROMPT);
	}

	public EvaluatorOptimizer(ChatClient chatClient, String generatorPrompt, String evaluatorPrompt) {
		Assert.notNull(chatClient, "ChatClient não pode ser nulo");
		Assert.hasText(generatorPrompt, "Prompt do gerador não pode ser vazio");
		Assert.hasText(evaluatorPrompt, "Prompt do avaliador não pode ser vazio");

		this.chatClient = chatClient;
		this.generatorPrompt = generatorPrompt;
		this.evaluatorPrompt = evaluatorPrompt;
	}

	public RefinedResponse loop(String task) {
		List<String> memory = new ArrayList<>();
		List<Generation> chainOfThought = new ArrayList<>();

		return loop(task, "", memory, chainOfThought);
	}

	private RefinedResponse loop(String task, String context, List<String> memory,
			List<Generation> chainOfThought) {

		Generation generation = generate(task, context);
		memory.add(generation.response());
		chainOfThought.add(generation);

		EvaluationResponse evaluationResponse = evalute(generation.response(), task);

		if (evaluationResponse.evaluation().equals(EvaluationResponse.Evaluation.PASS)) {
			return new RefinedResponse(generation.response(), chainOfThought);
		}

		StringBuilder newContext = new StringBuilder();
		newContext.append("Tentativas anteriores:");
		for (String m : memory) {
			newContext.append("\n- ").append(m);
		}
		newContext.append("\nFeedback: ").append(evaluationResponse.feedback());

		return loop(task, newContext.toString(), memory, chainOfThought);
	}

	private Generation generate(String task, String context) {
		Generation generationResponse = chatClient.prompt()
				.user(u -> u.text("{prompt}\n{context}\nTarefa: {task}")
						.param("prompt", this.generatorPrompt)
						.param("context", context)
						.param("task", task))
				.call()
				.entity(Generation.class);

		System.out.println(String.format("\n=== SAÍDA DO GERADOR ===\nPENSAMENTOS: %s\n\nRESPOSTA:\n %s\n",
				generationResponse.thoughts(), generationResponse.response()));
		return generationResponse;
	}

	private EvaluationResponse evalute(String content, String task) {
		EvaluationResponse evaluationResponse = chatClient.prompt()
				.user(u -> u.text("{prompt}\nTarefa original: {task}\nConteúdo a avaliar: {content}")
						.param("prompt", this.evaluatorPrompt)
						.param("task", task)
						.param("content", content))
				.call()
				.entity(EvaluationResponse.class);

		System.out.println(String.format("\n=== SAÍDA DO AVALIADOR ===\nAVALIAÇÃO: %s\n\nFEEDBACK: %s\n",
				evaluationResponse.evaluation(), evaluationResponse.feedback()));
		return evaluationResponse;
	}

}
