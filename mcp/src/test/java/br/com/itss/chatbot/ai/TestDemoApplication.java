package br.com.itss.chatbot.ai;

import org.springframework.boot.SpringApplication;

public class TestDemoApplication {
 
    public static void main(String[] args) {
        SpringApplication.from(Main::main)
            //.with(TestcontainersConfiguration.class, IngestionConfiguration.class, GrafanaContainerConfiguration.class)
            .with(TestcontainersConfiguration.class, IngestionConfiguration.class)
            .run(args);
    }
 
}