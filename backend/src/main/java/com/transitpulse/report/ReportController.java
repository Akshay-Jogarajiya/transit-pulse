package com.transitpulse.report;

import com.transitpulse.analysis.AnalysisEngine;
import com.transitpulse.analysis.AnalysisResult;
import com.transitpulse.ingestion.UploadSessionService;
import com.transitpulse.model.BriefDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
public class ReportController {

    private final AnalysisEngine analysisEngine;
    private final BriefAssembler briefAssembler;
    private final UploadSessionService session;

    public ReportController(AnalysisEngine analysisEngine, BriefAssembler briefAssembler, UploadSessionService session) {
        this.analysisEngine = analysisEngine;
        this.briefAssembler = briefAssembler;
        this.session = session;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        String state = session.hasTodayData() ? "READY" : "IDLE";
        return ResponseEntity.ok(Map.of("state", state));
    }

    @PostMapping("/run")
    public ResponseEntity<?> run(@RequestBody(required = false) Map<String, String> body) {
        if (!session.hasTodayData()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No today data uploaded. Please upload at least one of Metro, BRTS, or AMTS CSV files."));
        }

        LocalDate reportDate;
        try {
            String dateStr = body != null ? body.get("reportDate") : null;
            reportDate = dateStr != null ? LocalDate.parse(dateStr) : inferReportDate();
        } catch (Exception e) {
            reportDate = inferReportDate();
        }

        AnalysisResult analysis = analysisEngine.analyze(
                session.getMetroToday(), session.getMetroBaseline(),
                session.getBrtsToday(), session.getBrtsBaseline(),
                session.getAmtsToday(), session.getAmtsBaseline(),
                session.getIncidents(), session.getWeather()
        );

        // Merge upload warnings into the analysis data-quality warnings
        java.util.List<String> allWarnings = new java.util.ArrayList<>(session.getUploadWarnings());
        allWarnings.addAll(analysis.dataQualityWarnings());

        AnalysisResult analysisWithWarnings = new com.transitpulse.analysis.AnalysisResult(
                analysis.networkSummary(), analysis.systemSummaries(),
                analysis.topExceptions(), analysis.totalExceptionsFound(), allWarnings);

        BriefDto brief = briefAssembler.assemble(reportDate, analysisWithWarnings);
        return ResponseEntity.ok(brief);
    }

    private LocalDate inferReportDate() {
        // Try to get date from uploaded today's records
        return session.getMetroToday().stream().findFirst()
                .map(r -> r.date())
                .orElseGet(() -> session.getBrtsToday().stream().findFirst()
                        .map(r -> r.date())
                        .orElse(LocalDate.now()));
    }
}
