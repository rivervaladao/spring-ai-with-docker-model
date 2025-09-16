package br.com.itss.chatbot.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

@SpringBootTest(classes = { TestcontainersConfiguration.class, IngestionConfiguration.class },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RagControllerTest {
 
    @LocalServerPort
    private int port;
 
    @Autowired
    private VectorStore vectorStore;
 
    @Autowired
    private ChatClient.Builder chatClientBuilder;
 
    @Test
    void verifyTestcontainersAnswer() {
        var question = "Tell me about Testcontainers";
        var answer = retrieveAnswer(question);
 
        assertFactCheck(question, answer);
    }
 
    private String retrieveAnswer(String question) {
        RestClient restClient = RestClient.builder().baseUrl("http://localhost:%d".formatted(this.port)).build();
        return restClient.get().uri("/rag?message={question}", question).retrieve().body(String.class);
    }
 
    private void assertFactCheck(String question, String answer) {
        FactCheckingEvaluator factCheckingEvaluator = new FactCheckingEvaluator(this.chatClientBuilder);
        EvaluationResponse evaluate = factCheckingEvaluator.evaluate(new EvaluationRequest(docs(question), answer));
        assertTrue(evaluate.isPass());
    }
 
    private List<Document> docs(String question) {
        var response = RagController
            .callResponseSpec(this.chatClientBuilder.build(), this.vectorStore, question)
            .chatResponse();
        return response.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
    }
 
}