package com.transitpulse.report;

import com.transitpulse.ai.AiNarrativeService;
import com.transitpulse.ai.NarrativeResult;
import com.transitpulse.analysis.AnalysisResult;
import com.transitpulse.model.BriefDto;
import com.transitpulse.model.ExceptionDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BriefAssembler {

    private final AiNarrativeService aiNarrativeService;

    public BriefAssembler(AiNarrativeService aiNarrativeService) {
        this.aiNarrativeService = aiNarrativeService;
    }

    public BriefDto assemble(LocalDate reportDate, AnalysisResult analysis) {
        NarrativeResult narrative = aiNarrativeService.generate(reportDate, analysis);

        List<String> actions = buildActions(analysis.topExceptions());

        return new BriefDto(
                reportDate,
                LocalDateTime.now(),
                analysis.networkSummary(),
                analysis.systemSummaries(),
                analysis.topExceptions(),
                actions,
                narrative.text(),
                narrative.source(),
                analysis.dataQualityWarnings(),
                true
        );
    }

    private List<String> buildActions(List<ExceptionDto> exceptions) {
        List<String> actions = new ArrayList<>();
        for (ExceptionDto ex : exceptions) {
            String action = switch (ex.severity()) {
                case HIGH -> "Urgently verify operational status and investigate incident logs at " + ex.entityName() + ".";
                case MEDIUM -> "Review ridership and service data for " + ex.entityName() + "; check for unreported disruptions.";
                case LOW -> "Monitor " + ex.entityName() + " over the next 24 hours for further anomalies.";
            };
            actions.add(action);
        }
        return actions;
    }
}
