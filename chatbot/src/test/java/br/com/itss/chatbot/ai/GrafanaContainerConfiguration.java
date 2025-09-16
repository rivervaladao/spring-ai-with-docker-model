package br.com.itss.chatbot.ai;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.grafana.LgtmStackContainer;

@TestConfiguration(proxyBeanMethods = false)
public class GrafanaContainerConfiguration {
 
    @Bean
    @ServiceConnection
    LgtmStackContainer lgtmContainer() {
        return new LgtmStackContainer("grafana/otel-lgtm:0.11.4");
    }
 
}