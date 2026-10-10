package com.transitpulse.analysis;

import com.transitpulse.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalysisEngine {

    @Value("${transit.pulse.analysis.alert-decline-pct:15.0}")
    private double alertDeclinePct;

    @Value("${transit.pulse.analysis.warning-decline-pct:8.0}")
    private double warningDeclinePct;

    @Value("${transit.pulse.analysis.alert-surge-pct:20.0}")
    private double alertSurgePct;

    @Value("${transit.pulse.analysis.warning-surge-pct:12.0}")
    private double warningSurgePct;

    @Value("${transit.pulse.analysis.schedule-alert-pct:85.0}")
    private double scheduleAlertPct;

    @Value("${transit.pulse.analysis.min-baseline-passengers:100}")
    private int minBaselinePassengers;

    @Value("${transit.pulse.analysis.max-exceptions-in-brief:5}")
    private int maxExceptions;

    private final NormalizationService normalizationService;

    public AnalysisEngine(NormalizationService normalizationService) {
        this.normalizationService = normalizationService;
    }

    public AnalysisResult analyze(
            List<DailyRidershipRecord> metroToday, List<DailyRidershipRecord> metroBaseline,
            List<DailyRidershipRecord> brtsToday, List<DailyRidershipRecord> brtsBaseline,
            List<DailyRidershipRecord> amtsToday, List<DailyRidershipRecord> amtsBaseline,
            List<IncidentRecord> incidents, List<WeatherRecord> weather) {

        // Enrich today's records with incident flags
        metroToday = normalizationService.enrich(metroToday, incidents);
        brtsToday = normalizationService.enrich(brtsToday, incidents);
        amtsToday = normalizationService.enrich(amtsToday, incidents);

        // Build baseline indices
        Map<String, DailyRidershipRecord> metroBase = normalizationService.indexByEntityId(metroBaseline);
        Map<String, DailyRidershipRecord> brtsBase = normalizationService.indexByEntityId(brtsBaseline);
        Map<String, DailyRidershipRecord> amtsBase = normalizationService.indexByEntityId(amtsBaseline);

        // Detect weather flag
        boolean heavyRain = weather.stream().anyMatch(w ->
                w.rainfallMm() != null && w.rainfallMm() >= 15.0 ||
                ("HEAVY_RAIN".equals(w.weatherCondition()) || "STORM".equals(w.weatherCondition()))
        );
        double rainfallMm = weather.stream()
                .mapToDouble(w -> w.rainfallMm() != null ? w.rainfallMm() : 0.0)
                .max().orElse(0.0);
        String weatherNote = heavyRain
                ? String.format("Heavy rainfall recorded (%.1f mm)", rainfallMm)
                : (rainfallMm > 0 ? String.format("Light rainfall recorded (%.1f mm)", rainfallMm) : null);

        // Build exception list
        List<ExceptionDto> exceptions = new ArrayList<>();
        exceptions.addAll(detectExceptions(metroToday, metroBase, incidents, heavyRain, weatherNote));
        exceptions.addAll(detectExceptions(brtsToday, brtsBase, incidents, heavyRain, weatherNote));
        exceptions.addAll(detectExceptions(amtsToday, amtsBase, incidents, heavyRain, weatherNote));

        // Rank: severity_score = abs(pctChange) * log10(max(total,1))
        // Then stable-sort by system priority within same score bucket
        exceptions.sort((a, b) -> {
            double scoreA = severityScore(a);
            double scoreB = severityScore(b);
            int cmp = Double.compare(scoreB, scoreA);
            if (cmp != 0) return cmp;
            return systemPriority(a.system()) - systemPriority(b.system());
        });

        List<ExceptionDto> topExceptions = exceptions.stream().limit(maxExceptions).collect(Collectors.toList());

        // System summaries
        SystemSummaryDto metroSummary = buildSystemSummary(TransportSystem.METRO, metroToday, metroBaseline);
        SystemSummaryDto brtsSummary = buildSystemSummary(TransportSystem.BRTS, brtsToday, brtsBaseline);
        SystemSummaryDto amtsSummary = buildSystemSummary(TransportSystem.AMTS, amtsToday, amtsBaseline);

        int networkToday = metroSummary.totalToday() + brtsSummary.totalToday() + amtsSummary.totalToday();
        int networkYesterday = metroSummary.totalYesterday() + brtsSummary.totalYesterday() + amtsSummary.totalYesterday();
        Double networkPct = networkYesterday > 0 ? pct(networkToday - networkYesterday, networkYesterday) : null;

        NetworkSummaryDto networkSummary = new NetworkSummaryDto(networkToday, networkYesterday, networkPct);

        // Data quality warnings
        List<String> dqWarnings = new ArrayList<>();
        if (metroBaseline.isEmpty()) dqWarnings.add("Metro baseline data not loaded — no day-over-day comparison available.");
        if (brtsBaseline.isEmpty()) dqWarnings.add("BRTS baseline data not loaded — no day-over-day comparison available.");
        if (amtsBaseline.isEmpty()) dqWarnings.add("AMTS baseline data not loaded — no day-over-day comparison available.");
        if (incidents.isEmpty()) dqWarnings.add("No incident data loaded — anomaly evidence limited to correlations only.");
        if (weather.isEmpty()) dqWarnings.add("No weather data loaded — weather correlations not available.");

        return new AnalysisResult(networkSummary, List.of(metroSummary, brtsSummary, amtsSummary),
                topExceptions, exceptions.size(), dqWarnings);
    }

    private List<ExceptionDto> detectExceptions(List<DailyRidershipRecord> todayRecords,
                                                  Map<String, DailyRidershipRecord> baselineIndex,
                                                  List<IncidentRecord> incidents,
                                                  boolean heavyRain, String weatherNote) {
        List<ExceptionDto> result = new ArrayList<>();

        // Build incident lookup: entityId -> list of incidents
        Map<String, List<IncidentRecord>> incidentByEntity = new HashMap<>();
        for (IncidentRecord inc : incidents) {
            incidentByEntity.computeIfAbsent(inc.entityId(), k -> new ArrayList<>()).add(inc);
        }

        for (DailyRidershipRecord today : todayRecords) {
            DailyRidershipRecord baseline = baselineIndex.get(today.entityId());

            // Ridership anomaly check
            if (baseline != null && baseline.totalPassengers() >= minBaselinePassengers) {
                double change = pct(today.totalPassengers() - baseline.totalPassengers(), baseline.totalPassengers());
                Severity severity = null;

                if (change <= -alertDeclinePct) severity = Severity.HIGH;
                else if (change <= -warningDeclinePct) severity = Severity.MEDIUM;
                else if (change >= alertSurgePct) severity = Severity.HIGH;
                else if (change >= warningSurgePct) severity = Severity.MEDIUM;

                if (severity != null) {
                    List<EvidenceItem> evidence = buildEvidence(today, baseline, incidents, incidentByEntity, heavyRain, weatherNote, change);
                    result.add(new ExceptionDto(today.entityId(), today.entityName(), today.system(),
                            "ridership", today.totalPassengers(), baseline.totalPassengers(),
                            round2(change), severity, evidence));
                }
            }

            // Schedule adherence anomaly (BRTS/AMTS only)
            if (today.scheduledUnits() != null && today.scheduledUnits() > 0) {
                double adherence = 100.0 * today.operationalUnits() / today.scheduledUnits();
                if (adherence < scheduleAlertPct) {
                    List<EvidenceItem> evidence = new ArrayList<>();
                    evidence.add(new EvidenceItem(EvidenceType.FACT,
                            String.format("Schedule adherence: %.1f%% (%d of %d scheduled trips operated)",
                                    adherence, today.operationalUnits(), today.scheduledUnits())));
                    List<IncidentRecord> entityIncidents = incidentByEntity.getOrDefault(today.entityId(), List.of());
                    for (IncidentRecord inc : entityIncidents) {
                        evidence.add(new EvidenceItem(EvidenceType.INCIDENT,
                                inc.incidentType() + ": " + inc.description() + " [" + inc.severity() + "]"));
                    }
                    // Only add schedule exception if not already covered by ridership exception for same entity
                    boolean alreadyAdded = result.stream().anyMatch(e -> e.entityId().equals(today.entityId()));
                    if (!alreadyAdded) {
                        result.add(new ExceptionDto(today.entityId(), today.entityName(), today.system(),
                                "schedule_adherence", today.operationalUnits(),
                                today.scheduledUnits(), round2(adherence - 100.0),
                                Severity.MEDIUM, evidence));
                    }
                }
            }
        }

        return result;
    }

    private List<EvidenceItem> buildEvidence(DailyRidershipRecord today, DailyRidershipRecord baseline,
                                              List<IncidentRecord> allIncidents,
                                              Map<String, List<IncidentRecord>> incidentByEntity,
                                              boolean heavyRain, String weatherNote, double pctChange) {
        List<EvidenceItem> evidence = new ArrayList<>();

        evidence.add(new EvidenceItem(EvidenceType.FACT,
                String.format("Ridership today: %,d vs baseline: %,d (%.1f%%)",
                        today.totalPassengers(), baseline.totalPassengers(), pctChange)));

        List<IncidentRecord> entityIncidents = incidentByEntity.getOrDefault(today.entityId(), List.of());
        for (IncidentRecord inc : entityIncidents) {
            String desc = inc.incidentType().replace("_", " ") + ": " + inc.description();
            if (!inc.startTime().isBlank()) desc += " [" + inc.startTime() + "–" + inc.endTime() + "]";
            evidence.add(new EvidenceItem(EvidenceType.INCIDENT, desc));
            evidence.add(new EvidenceItem(EvidenceType.POSSIBLE_CAUSE,
                    inc.incidentType().replace("_", " ").toLowerCase() + " co-occurred with ridership decline at this " +
                    systemEntityLabel(today.system())));
        }

        if (heavyRain && weatherNote != null && pctChange < -warningDeclinePct) {
            evidence.add(new EvidenceItem(EvidenceType.CORRELATION, weatherNote));
        }

        if (entityIncidents.isEmpty() && !heavyRain) {
            evidence.add(new EvidenceItem(EvidenceType.UNCONFIRMED, "No incident or weather evidence found for this anomaly."));
        }

        return evidence;
    }

    private SystemSummaryDto buildSystemSummary(TransportSystem system,
                                                 List<DailyRidershipRecord> today,
                                                 List<DailyRidershipRecord> baseline) {
        int totalToday = today.stream().mapToInt(DailyRidershipRecord::totalPassengers).sum();
        int totalYesterday = baseline.stream().mapToInt(DailyRidershipRecord::totalPassengers).sum();
        Double pctChange = totalYesterday > 0 ? round2(pct(totalToday - totalYesterday, totalYesterday)) : null;

        // Schedule adherence: only meaningful for BRTS/AMTS
        Double scheduleAdherence = null;
        if (system != TransportSystem.METRO) {
            long withSchedule = today.stream().filter(r -> r.scheduledUnits() != null && r.scheduledUnits() > 0).count();
            if (withSchedule > 0) {
                int operated = today.stream().filter(r -> r.scheduledUnits() != null).mapToInt(DailyRidershipRecord::operationalUnits).sum();
                int scheduled = today.stream().filter(r -> r.scheduledUnits() != null).mapToInt(r -> r.scheduledUnits()).sum();
                if (scheduled > 0) scheduleAdherence = round2(100.0 * operated / scheduled);
            }
        }

        return new SystemSummaryDto(system, totalToday, totalYesterday, pctChange, scheduleAdherence);
    }

    private double pct(int change, int baseline) {
        return 100.0 * change / baseline;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private double severityScore(ExceptionDto e) {
        if (e.pctChange() == null) return 0;
        return Math.abs(e.pctChange()) * Math.log10(Math.max(e.valueToday(), 1));
    }

    private int systemPriority(TransportSystem s) {
        return switch (s) { case METRO -> 0; case BRTS -> 1; case AMTS -> 2; };
    }

    private String systemEntityLabel(TransportSystem s) {
        return switch (s) { case METRO -> "station"; case BRTS -> "route"; case AMTS -> "route"; };
    }
}
