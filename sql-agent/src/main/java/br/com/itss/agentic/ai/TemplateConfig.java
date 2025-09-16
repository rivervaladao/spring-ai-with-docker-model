package br.com.itss.agentic.ai;

import org.springframework.ai.template.TemplateRenderer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.template.st.StTemplateRenderer;

@Configuration
public class TemplateConfig {

    @Bean
    TemplateRenderer templateRenderer() {
        return StTemplateRenderer.builder()
                .startDelimiterToken('<')  // usa <param>
                .endDelimiterToken('>')    // ... e >
                .build();
    }
}