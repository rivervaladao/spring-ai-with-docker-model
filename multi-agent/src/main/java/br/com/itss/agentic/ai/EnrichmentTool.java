package br.com.itss.agentic.ai;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class EnrichmentTool {
    @Tool
    public String enrichWithExample(String text) {
        // Tool mock: Adiciona um exemplo de código ao texto
        return text + "\n\nExemplo de código: " +
                "List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);\n" +
                "List<Integer> result = list.stream()\n" +
                "    .filter(x -> x > 2)\n" +
                "    .map(x -> x * 2)\n" +
                "    .collect(Collectors.toList());\n" +
                "// Resultado: [6, 8, 10]";
    }
}
