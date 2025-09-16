package br.com.itss.agentic.ai.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Ferramenta para propor gráficos (especificações conceituais).
 */
@Component
public class ChartTools {

    private static final Logger log = LoggerFactory.getLogger(ChartTools.class);

    @Tool(
            name = "propose_charts",
            description = """
            Sugere até 2 gráficos úteis com título, tipo e campos de dados.
            Parâmetros:
            - goal (obrigatório)
            - notes (opcional)
            """
    )
    public String proposeCharts(String goal, String notes) {
        long t0 = System.currentTimeMillis();
        log.info("[TOOL] propose_charts START goalPreview='{}' notesLen={}",
                preview(goal, 60), notes == null ? 0 : notes.length());

        String out = """
               - Título: Participação de Mercado por Segmento
                 Tipo: barra
                 Dados: [segmento, participacao_percentual]
                 Insight: comparar liderança e nichos

               - Título: Evolução Trimestral da Adoção do Produto
                 Tipo: linha
                 Dados: [trimestre, adocao_percentual]
                 Insight: evidenciar tendência e sazonalidade
               """;

        log.info("[TOOL] propose_charts END outLen={} elapsedMs={}", out.length(), (System.currentTimeMillis() - t0));
        return out;
    }

    private static String preview(String s, int max) {
        if (s == null) return "";
        var t = s.strip();
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }
}
