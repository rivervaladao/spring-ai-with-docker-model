package br.com.itss.chatbot.ai;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.qdrant.QdrantContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	QdrantContainer qdrantContainer() {
		return new QdrantContainer(DockerImageName.parse("qdrant/qdrant:v1.13.0"));
	}

}
