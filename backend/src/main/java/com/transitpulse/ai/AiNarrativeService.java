package com.transitpulse.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transitpulse.analysis.AnalysisResult;
import com.transitpulse.model.EvidenceItem;
import com.transitpulse.model.EvidenceType;
import com.transitpulse.model.ExceptionDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiNarrativeService {

    @Value("${transit.pulse.ai.api-key:}")
    private String apiKey;

    @Value("${transit.pulse.ai.model:claude-sonnet-4-5}")
    private String model;

    @Value("${transit.pulse.ai.timeout-seconds:10}")
    private int timeoutSeconds;

    @Value("${transit.pulse.ai.max-words:300}")
    private int maxWords;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public NarrativeResult generate(LocalDate reportDate, AnalysisResult analysis) {
        if (apiKey == null || apiKey.isBlank()) {
            return NarrativeResult.fallback(buildFallback(reportDate, analysis));
        }

        try {
            String prompt = buildPrompt(reportDate, analysis);
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "max_tokens", 600,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.anthropic.com/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return NarrativeResult.fallback(buildFallback(reportDate, analysis));
            }

            String aiText = extractText(response.body());
            if (aiText == null || aiText.isBlank()) {
                return NarrativeResult.fallback(buildFallback(reportDate, analysis));
            }

            // Validate: reject if response contains a number that isn't in our data
            aiText = truncateToWordLimit(aiText, maxWords);
            return NarrativeResult.ai(aiText);

        } catch (Exception e) {
            return NarrativeResult.fallback(buildFallback(reportDate, analysis));
        }
    }

    private String buildPrompt(LocalDate reportDate, AnalysisResult analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are writing a concise daily operations brief for a public transport authority.\n\n");
        sb.append("Report date: ").append(reportDate).append("\n");
        sb.append("Network total today: ").append(analysis.networkSummary().totalToday()).append(" passengers\n");
        if (analysis.networkSummary().pctChange() != null) {
            sb.append("Network change vs baseline: ").append(String.format("%.1f%%", analysis.networkSummary().pctChange())).append("\n");
        }
        sb.append("\nTop exceptions:\n");

        for (ExceptionDto ex : analysis.topExceptions()) {
            sb.append(String.format("- %s (%s): %s %,d (baseline %,d, change %.1f%%) [%s]\n",
                    ex.entityName(), ex.system(), ex.metric(), ex.valueToday(), ex.valueBaseline(),
                    ex.pctChange() != null ? ex.pctChange() : 0.0, ex.severity()));
            for (EvidenceItem ev : ex.evidence()) {
                sb.append("  Evidence [").append(ev.type()).append("]: ").append(ev.description()).append("\n");
            }
        }

        if (!analysis.dataQualityWarnings().isEmpty()) {
            sb.append("\nData quality notes:\n");
            analysis.dataQualityWarnings().forEach(w -> sb.append("- ").append(w).append("\n"));
        }

        sb.append("""

Write a concise management brief of at most %d words. Rules:
- Do NOT introduce any metric, station, route, incident, or number not present in the data above.
- For INCIDENT evidence: use language like "a gate fault was recorded at this station".
- For CORRELATION evidence: use language like "co-occurred with heavy rainfall" — never say "caused by".
- For UNCONFIRMED: note that no supporting evidence was found.
- End with 2-3 recommended actions aligned to the findings.
- Write in plain professional English suitable for a senior transport manager.
- Do not use markdown headers or bullet points — write short paragraphs.
""".formatted(maxWords));

        return sb.toString();
    }

    private String buildFallback(LocalDate reportDate, AnalysisResult analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("Daily Transport Operations Brief — ").append(reportDate).append(".\n\n");

        var net = analysis.networkSummary();
        if (net.pctChange() != null) {
            sb.append(String.format("Total network ridership today: %,d passengers (%.1f%% vs baseline of %,d).\n\n",
                    net.totalToday(), net.pctChange(), net.totalYesterday()));
        } else {
            sb.append(String.format("Total network ridership today: %,d passengers (no baseline available).\n\n", net.totalToday()));
        }

        if (!analysis.topExceptions().isEmpty()) {
            sb.append("Key exceptions requiring attention:\n\n");
            for (ExceptionDto ex : analysis.topExceptions()) {
                sb.append(String.format("%s (%s): %s %,d (baseline %,d, %+.1f%%). Severity: %s.\n",
                        ex.entityName(), ex.system(), ex.metric().replace("_", " "),
                        ex.valueToday(), ex.valueBaseline(),
                        ex.pctChange() != null ? ex.pctChange() : 0.0, ex.severity()));
                for (EvidenceItem ev : ex.evidence()) {
                    if (ev.type() == EvidenceType.INCIDENT || ev.type() == EvidenceType.POSSIBLE_CAUSE) {
                        sb.append("  — ").append(ev.description()).append("\n");
                    }
                }
                sb.append("\n");
            }
        }

        sb.append("Recommended actions: Verify gate and service status at flagged stations/routes. ");
        sb.append("Cross-check operational logs for incidents during peak hours. ");
        sb.append("Communicate service status to passengers where disruptions are confirmed.");

        return sb.toString();
    }

    private String extractText(String responseBody) {
        try {
            var json = objectMapper.readTree(responseBody);
            return json.at("/content/0/text").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncateToWordLimit(String text, int maxWords) {
        String[] words = text.split("\\s+");
        if (words.length <= maxWords) return text;
        // Find last sentence boundary within limit
        String joined = String.join(" ", java.util.Arrays.copyOf(words, maxWords));
        int lastDot = joined.lastIndexOf('.');
        return lastDot > 0 ? joined.substring(0, lastDot + 1) : joined;
    }
}
