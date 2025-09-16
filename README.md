# Pré-requisitos

* java 21
* spring boot 
* docker engine(linux) /desktop(windows,mac)
* docker model plugin 
* docker mcp 
* Api Key Tavily

# Docker 
## Configurando o Docker Model Runner
Veja [`documentação oficial`](https://docs.docker.com/ai/model-runner/)

* Modelo usado para chatcompletions

```bash
docker model pull ai/llama3.1
```
* Modelo usado para embedding

```bash
docker model pull ai/mxbai-embed-large
```
Os modelos são hospedados no Docker Hub sob o namespace [`ai`](https://hub.docker.com/u/ai)
## Configurando o Docker CMP 
* Engine Gateway (Linux)

[`Instalar o MCP Gateway pre-built`](https://docs.docker.com/ai/mcp-gateway/#install-using-a-pre-built-binary)

* Docker Desktop (Windows/MacOs)

[`Habilitar recurso MCP Toolkit`](https://docs.docker.com/ai/mcp-catalog-and-toolkit/toolkit/#enable-docker-mcp-toolkit)

# Demos
## ChatController
Chatbot: simples implementação de chat usando docker model como openai api usando llma model

#### springboot mvn run

```bash
./mvnw spring-boot:test-run
```

## RagController
RAG: implementação de um chatbot usando docker model como openai api usando llma model tranzendo dados de uma base vetorial

### Testcontainers support

This project uses [Testcontainers at development time](https://docs.spring.io/spring-boot/3.5.4/reference/features/dev-services.html#features.dev-services.testcontainers).

Testcontainers has been configured to use the following Docker images:

#### Banco Vetorial Qdrant
* [`qdrant/qdrant:latest`](https://hub.docker.com/r/qdrant/qdrant)

#### Observabilidade para debug com Open Telemetry/Graphana
* [`grafana/otel-lgtm`](https://hub.docker.com/r/grafana/otel-lgtm)

#### springboot mvn run
```bash
./mvnw spring-boot:test-run
```

## McpSTDIOController
* Implementação de um chatbot usando docker model como openai api usando llma model trazendo 
* MCP Client usando comunicação STDIO

[`Tavily MCP`](https://docs.tavily.com/documentation/mcp#tavily-extract-examples)dados de um servidor mcp tavily

tools:

* extract
* search

#### run mcp server

```bash
TAVILY_API_KEY=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 npx -y tavily-mcp@latest
```

#### springboot mvn run

```bash
./mvnw spring-boot:run
```
#### test httpie

```bash
 http :8080/mcp/tavily message=="extrair todo texto possivel de https://pmrun.com.br/solucoes/pm-run-para-monitoramento/?"
```

## McpSSEController
* Implementação de um chatbot usando docker model como openai api usando llma model trazendo 
* MCP Client usando comunicação SSE
* Usando docker gateway
* [`mcp/postgres`](https://hub.docker.com/mcp/server/postgres/) 

tools:
* query

#### run docker mcp gateway
* obs:  transport streaming | sse

```bash
docker mcp gateway run \
--log-calls=true \
--port=8811 \
--transport=sse\
--servers=postgres \
--tools=query \
--verbose=true \
--secrets=./postgres_url.env
```

#### springboot mvn run

```bash
./mvnw spring-boot:run
```

#### test httpie

```bash
 http :8080/mcp/postgresql message="liste as 5 top ordens de servico"
```