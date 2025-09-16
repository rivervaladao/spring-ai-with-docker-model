package br.com.itss.agentic.ai;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import br.com.itss.agentic.ai.Models.AgentResponse;

@Service
class MultiAgentService {

    private final ChatClient chatClient;
    private final EnrichmentTool enrichmentTool;

    @Autowired
    public MultiAgentService(ChatClient.Builder chatClient, EnrichmentTool enrichmentTool) {
        this.chatClient = chatClient.build();
        this.enrichmentTool = enrichmentTool;
    }

    public AgentResponse executeMultiAgent(String topic) {
        try {
            // Agente de Pesquisa: Prompt estruturado para explicação detalhada (até 200 palavras)
            SystemPromptTemplate researchTemplate = new SystemPromptTemplate(
                    "Você é um assistente de pesquisa. Forneça uma explicação detalhada sobre {topic} em até 200 palavras.");
            Prompt researchPrompt = researchTemplate.create(Map.of("topic", topic));
            ChatResponse researchResponse = chatClient.prompt(researchPrompt).call().chatResponse();
            Generation researchGeneration = researchResponse.getResult();
            String researchResult = researchGeneration.getOutput().getText();

            if (researchResult == null || researchResult.isEmpty()) {
                throw new RuntimeException("Nenhuma resposta recebida do agente de pesquisa.");
            }

            // Demonstração de Function Calling: Chama uma tool para enriquecer (ex.: adicionar exemplo de código)
            String enrichedResearch = enrichmentTool.enrichWithExample(researchResult);

            // Agente de Resumo: Resume o resultado enriquecido em até 50 palavras
            SystemPromptTemplate summaryTemplate = new SystemPromptTemplate(
                    "Você é um assistente de resumo. Resuma o texto a seguir em 50 palavras ou menos: {text}");
            Prompt summaryPrompt = summaryTemplate.create(Map.of("text", enrichedResearch));
            ChatResponse summaryResponse = chatClient.prompt(summaryPrompt).call().chatResponse();
            Generation summaryGeneration = summaryResponse.getResult();
            String summaryResult = summaryGeneration.getOutput().getText();

            Usage researchUsage = researchResponse.getMetadata().getUsage();
            if (researchUsage != null) {
                System.out.println("Tokens de pesquisa: " + researchUsage.getTotalTokens());
            }

            return new AgentResponse(researchResult, summaryResult);
        } catch (Exception e) {
            return new AgentResponse("Erro: " + e.getMessage(), "");
        }
    }

}