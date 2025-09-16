package br.com.itss.agentic.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;


public class HierarchicalTeam {
    private static final Logger log = LoggerFactory.getLogger(HierarchicalTeam.class);

    private final Agents agents;

    public HierarchicalTeam(ChatClient chat) {
        this.agents = new Agents(chat);
    }

    public Result run(String userGoal, int maxTurns) {
        var notes = new ArrayList<String>();
        var cites = new ArrayList<String>();
        var charts = new ArrayList<String>();
        var draft = "";

        var state = new Models.TeamState(userGoal, notes, cites, charts, draft);

        log.info("▶️ Start HierarchicalTeam.run goal='{}' maxTurns={}", userGoal, maxTurns);

        for (int turn = 1; turn <= maxTurns; turn++) {
            log.info("---- TURN {} -------------------------------------------------", turn);

            var sup = agents.supervisor(state);
            log.info("🧭 Supervisor decision: next='{}' reason='{}'", sup.next(), sup.reason());

            switch (sup.next()) {
                case "RESEARCH_TEAM" -> {
                    var r = agents.researchRouter(state);
                    log.info("🔎 Research router: next='{}' reason='{}'", r.next(), r.reason());
                    switch (r.next()) {
                        case "SEARCHER" -> {
                            var w = agents.searcher(userGoal);
                            notes.add("Pesquisa:\n" + w.content());
                            log.info("✅ SEARCHER appended notes (len={} chars)", w.content().length());
                        }
                        case "WEB_SCRAPER" -> {
                            var w = agents.webScraper(userGoal);
                            notes.add("Fontes:\n" + w.content());
                            log.info("✅ WEB_SCRAPER appended notes (len={} chars)", w.content().length());
                        }
                        case "RETURN" -> log.debug("↩️ ResearchTeam RETURN to Supervisor");
                        default -> log.warn("⚠️ Research router returned unknown branch '{}'", r.next());
                    }
                }
                case "DOCUMENT_AUTHORING" -> {
                    var r = agents.docAuthoringRouter(state);
                    log.info("📝 DocAuthoring router: next='{}' reason='{}'", r.next(), r.reason());
                    switch (r.next()) {
                        case "NOTE_TAKER" -> {
                            var w = agents.noteTaker(String.join("\n", notes));
                            notes.add("Notas Refinadas:\n" + preview(w.content(), 500));
                            log.info("✅ NOTE_TAKER refined notes (new total notes={})", notes.size());
                        }
                        case "WRITER" -> {
                            var w = agents.writer(userGoal, notes);
                            draft = w.content();
                            log.info("✅ WRITER updated draft (len={} chars)", draft.length());
                        }
                        case "CHART_GENERATOR" -> {
                            var w = agents.chartGen(userGoal, notes);
                            charts.add(w.content());
                            log.info("✅ CHART_GENERATOR proposed charts (count now={})", charts.size());
                        }
                        case "RETURN" -> log.debug("↩️ DocAuthoring RETURN to Supervisor");
                        default -> log.warn("⚠️ DocAuthoring router unknown branch '{}'", r.next());
                    }
                }
                case "FINISH" -> {
                    log.info("🏁 FINISH at turn {} (draftLen={}, notes={}, charts={})",
                            turn, draft.length(), notes.size(), charts.size());
                    return new Result(draft, notes, cites, charts, turn);
                }
                default -> log.warn("⚠️ Supervisor unknown branch '{}'", sup.next());
            }

            log.info("⏱️ Turn {}  | state: draftLen={}, notes={}, charts={}",
                    turn,  draft.length(), notes.size(), charts.size());
        }

        log.info("⛳ Max turns reached ({}). Returning current state.", maxTurns);
        return new Result(draft, notes, cites, charts, maxTurns);
    }
    private static String preview(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + " …(+" + (s.length() - max) + " chars)";
    }
    public record Result(String draft, List<String> notes, List<String> citations, List<String> charts, int turns) {}
}
