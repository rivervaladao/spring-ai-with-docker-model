package br.com.itss.agentic.ai;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

//@RestController
public class MultiAgentController {

    private final MultiAgentService multiAgentService;

    public MultiAgentController(MultiAgentService multiAgentService) {
        this.multiAgentService = multiAgentService;
    }

    @GetMapping("/multi-agent")
    public Models.AgentResponse runMultiAgent(@RequestParam(defaultValue = "Java streams") String topic) {
        return multiAgentService.executeMultiAgent(topic);
    }
}