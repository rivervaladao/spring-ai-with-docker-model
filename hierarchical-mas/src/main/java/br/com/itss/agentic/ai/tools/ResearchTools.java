package br.com.itss.agentic.ai.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Ferramentas de pesquisa e (mini) "scrape" HTTP.
 */
@Component
public class ResearchTools {

    private static final Logger log = LoggerFactory.getLogger(ResearchTools.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Busca simulada (stub). Substitua pela sua Search API (Bing, SerpAPI, etc.)
     */
    @Tool(
            name = "search_web",
            description = """
            Busca simples na Web e retorna títulos/sumários (use para mapear o tema).
            Parâmetros: 
            - query (obrigatório)
            - maxResults (opcional, padrão=5; máx=10)
            """
    )
    public List<String> searchWeb(String query, Integer maxResults) {
        long t0 = System.currentTimeMillis();
        int k = (maxResults == null || maxResults <= 0) ? 5 : Math.min(maxResults, 10);

        log.info("[TOOL] search_web START query='{}' maxResults={}", query, k);

        // Simulação: aqui você chamaria sua API de busca e normalizaria a saída
        List<String> out = List.of(
                "Panorama do mercado (fonte a definir)",
                "Tendências e materiais sustentáveis (fonte a definir)",
                "Preços e posicionamento competitivo (fonte a definir)",
                "Canais de distribuição e B2B (fonte a definir)",
                "Regulamentações ambientais (fonte a definir)"
        ).subList(0, Math.min(k, 5));

        log.info("[TOOL] search_web END items={} elapsedMs={}", out.size(), (System.currentTimeMillis() - t0));
        return out;
    }

    /**
     * GET simples e retorno de <title> + snippet do corpo.
     */
    @Tool(
            name = "scrape_url",
            description = """
            Faz um GET em uma URL pública e retorna o <title> e os 500 primeiros caracteres do corpo.
            Parâmetros:
            - url (obrigatório)
            """
    )
    public String scrapeUrl(String url) {
        long t0 = System.currentTimeMillis();
        log.info("[TOOL] scrape_url START url='{}'", url);

        try {
            var req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .GET()
                    .header("User-Agent", "SpringAI-Tools/1.0")
                    .build();

            var res = http.send(req, HttpResponse.BodyHandlers.ofString());
            int status = res.statusCode();
            String body = res.body() == null ? "" : res.body().replaceAll("\\s+", " ").trim();

            String title = body.replaceFirst("(?is).*?<title>(.*?)</title>.*", "$1");
            if (title.equals(body)) title = "(sem título)";

            String snippet = body.length() > 500 ? body.substring(0, 500) + "..." : body;

            String out = "Status: " + status + "\nTitle: " + title + "\nSnippet: " + snippet;

            log.info("[TOOL] scrape_url END status={} bodyLen={} elapsedMs={}",
                    status, body.length(), (System.currentTimeMillis() - t0));
            return out;
        } catch (Exception e) {
            log.warn("[TOOL] scrape_url ERROR url='{}' message={}", url, e.toString());
            return "Falha ao acessar '" + url + "': " + e.getMessage();
        }
    }
}
