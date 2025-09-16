package br.com.itss.agentic.ai;

import org.springframework.ai.chat.client.ChatClient;
import java.util.List;

// imports novos
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.time.Duration;
import br.com.itss.agentic.ai.Models.*;

public class Agents {
    private static final Logger log = LoggerFactory.getLogger(Agents.class);

    private final ChatClient chat;

    public Agents(ChatClient chat) { this.chat = chat; }

    public Decision supervisor(TeamState s) {
        Instant t0 = Instant.now();
        var out = chat.prompt()
                .user(u -> u.text(Prompts.SUPERVISOR_PROMPT)
                        .param("goal", s.userGoal())
                        .param("notes", String.join(" | ", s.notes()))
                        .param("draft", s.draft())
                        .param("cites", String.join(" | ", s.citations()))
                        .param("charts", String.join(" | ", s.charts())))
                .call()
                .entity(Decision.class);
        log.debug("LLM supervisor -> next='{}' ({} ms)", out.next(), Duration.between(t0, Instant.now()).toMillis());
        return out;
    }

    public Decision researchRouter(TeamState s) {
        Instant t0 = Instant.now();
        var out = chat.prompt()
                .user(u -> u.text(Prompts.RESEARCH_TEAM_ROUTER)
                        .param("goal", s.userGoal())
                        .param("notes", String.join(" | ", s.notes())))
                .call()
                .entity(Decision.class);
        log.debug("LLM researchRouter -> next='{}' ({} ms)", out.next(), Duration.between(t0, Instant.now()).toMillis());
        return out;
    }

    public Decision docAuthoringRouter(TeamState s) {
        Instant t0 = Instant.now();
        var out = chat.prompt()
                .user(u -> u.text(Prompts.DOCUMENT_ROUTER)
                        .param("goal", s.userGoal())
                        .param("draft", s.draft())
                        .param("notes", String.join(" | ", s.notes()))
                        .param("cites", String.join(" | ", s.citations())))
                .call()
                .entity(Decision.class);
        log.debug("LLM docAuthoringRouter -> next='{}' ({} ms)", out.next(), Duration.between(t0, Instant.now()).toMillis());
        return out;
    }

    public WorkerOutput searcher(String goal) {
        Instant t0 = Instant.now();
        var out = chat.prompt()
                .user(u -> u.text(Prompts.SEARCHER_PROMPT).param("goal", goal))
                .call()
                .content();
        log.debug("LLM SEARCHER contentLen={} ({} ms)", out.length(), Duration.between(t0, Instant.now()).toMillis());
        return new WorkerOutput("SEARCHER", out);
    }

    public WorkerOutput webScraper(String goal) {
        Instant t0 = Instant.now();
        var out = chat.prompt()
                .user(u -> u.text(Prompts.WEB_SCRAPER_PROMPT).param("goal", goal))
                .call()
                .content();
        log.debug("LLM WEB_SCRAPER contentLen={} ({} ms)", out.length(), Duration.between(t0, Instant.now()).toMillis());
        return new WorkerOutput("WEB_SCRAPER", out);
    }

    public WorkerOutput noteTaker(String content) {
        Instant t0 = Instant.now();
        var out = chat.prompt()
                .user(u -> u.text(Prompts.NOTE_TAKER_PROMPT).param("content", content))
                .call()
                .content();
        log.debug("LLM NOTE_TAKER contentLen={} ({} ms)", out.length(), Duration.between(t0, Instant.now()).toMillis());
        return new WorkerOutput("NOTE_TAKER", out);
    }

    public WorkerOutput writer(String goal, List<String> notes) {
        Instant t0 = Instant.now();
        var out = chat.prompt()
                .user(u -> u.text(Prompts.WRITER_PROMPT)
                        .param("goal", goal)
                        .param("notes", String.join("\n", notes)))
                .call()
                .content();
        log.debug("LLM WRITER contentLen={} ({} ms)", out.length(), Duration.between(t0, Instant.now()).toMillis());
        return new WorkerOutput("WRITER", out);
    }

    public WorkerOutput chartGen(String goal, List<String> notes) {
        Instant t0 = Instant.now();
        var out = chat.prompt()
                .user(u -> u.text(Prompts.CHART_GENERATOR_PROMPT)
                        .param("goal", goal)
                        .param("notes", String.join("\n", notes)))
                .call()
                .content();
        log.debug("LLM CHART_GENERATOR contentLen={} ({} ms)", out.length(), Duration.between(t0, Instant.now()).toMillis());
        return new WorkerOutput("CHART_GENERATOR", out);
    }
}
