package br.com.itss.chatbot.ai;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
 
    private final ChatClient chatClient;
 
    public ChatController(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }
 
    @GetMapping("/chat")
    public String generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return this.chatClient.prompt().user(message).call().content();
    }
 
}