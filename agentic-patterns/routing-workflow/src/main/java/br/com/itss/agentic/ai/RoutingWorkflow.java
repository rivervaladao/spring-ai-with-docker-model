package br.com.itss.agent.ai;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.Assert;

import br.com.itss.agentic.ai.RoutingResponse;

public class RoutingWorkflow {

    private final ChatClient chatClient;

    public RoutingWorkflow(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String route(String input, Map<String, String> routes) {
        Assert.notNull(input, "O texto de entrada não pode ser nulo");
        Assert.notEmpty(routes, "O mapa de rotas não pode ser nulo ou vazio");

        String routeKey = determineRoute(input, routes.keySet());

        String selectedPrompt = routes.get(routeKey);

        if (selectedPrompt == null) {
            throw new IllegalArgumentException("A rota selecionada '" + routeKey + "' não foi encontrada no mapa de rotas");
        }

        return chatClient.prompt(selectedPrompt + "\nEntrada: " + input).call().content();
    }

    @SuppressWarnings("null")
    private String determineRoute(String input, Iterable<String> availableRoutes) {
        System.out.println("\nRotas disponíveis: " + availableRoutes);

        String selectorPrompt = String.format("""
                Analise a entrada e selecione a equipe de suporte mais apropriada dentre estas opções: %s
                Primeiro explique seu raciocínio e, em seguida, forneça sua seleção neste formato JSON:

                \\{
                    "reasoning": "Breve explicação de por que este chamado deve ser roteado para uma equipe específica.
                                  Considere termos-chave, intenção do usuário e nível de urgência.",
                    "selection": "Nome da equipe escolhida"
                \\}

                Entrada: %s""", availableRoutes, input);

        RoutingResponse routingResponse = chatClient.prompt(selectorPrompt).call().entity(RoutingResponse.class);

        System.out.println(String.format("Análise de Roteamento:%s\nRota selecionada: %s",
                routingResponse.reasoning(), routingResponse.selection()));

        return routingResponse.selection();
    }
}
