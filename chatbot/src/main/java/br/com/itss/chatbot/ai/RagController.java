package br.com.itss.chatbot.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RagController {
 
    private final ChatClient chatClient;
 
    private final VectorStore vectorStore;
 
    public RagController(ChatModel chatModel, VectorStore vectorStore) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.vectorStore = vectorStore;
    }
 
    @GetMapping("/rag")
    public String generate(@RequestParam(value = "message", defaultValue = "What's Testcontainers?") String message) {
        return callResponseSpec(this.chatClient, this.vectorStore, message).content();
    }
 
    static ChatClient.CallResponseSpec callResponseSpec(ChatClient chatClient, VectorStore vectorStore,
            String question) {
        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(SearchRequest.builder().topK(1).build())
            .build();
        return chatClient.prompt().advisors(questionAnswerAdvisor).user(question).call();
    }
 
}