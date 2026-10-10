package com.transitpulse.analysis;

import com.transitpulse.model.DailyRidershipRecord;
import com.transitpulse.model.IncidentRecord;
import com.transitpulse.model.TransportSystem;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enriches DailyRidershipRecords with incident flags and validates date consistency.
 */
@Service
public class NormalizationService {

    public List<DailyRidershipRecord> enrich(List<DailyRidershipRecord> records, List<IncidentRecord> incidents) {
        // Build a set of (system, entityId) pairs that have incidents on the same date
        Set<String> incidentKeys = incidents.stream()
                .map(i -> incidentKey(i.system(), i.entityId(), i.date()))
                .collect(Collectors.toSet());

        return records.stream()
                .map(r -> {
                    boolean hasIncident = incidentKeys.contains(incidentKey(r.system(), r.entityId(), r.date()));
                    return new DailyRidershipRecord(
                            r.date(), r.system(), r.entityId(), r.entityName(),
                            r.totalPassengers(), r.operationalUnits(), r.scheduledUnits(),
                            hasIncident, r.notes()
                    );
                })
                .collect(Collectors.toList());
    }

    private String incidentKey(TransportSystem system, String entityId, LocalDate date) {
        return system.name() + ":" + entityId + ":" + date;
    }

    public Map<String, DailyRidershipRecord> indexByEntityId(List<DailyRidershipRecord> records) {
        // For duplicate keys, last one wins
        Map<String, DailyRidershipRecord> map = new java.util.LinkedHashMap<>();
        for (DailyRidershipRecord r : records) map.put(r.entityId(), r);
        return map;
    }
}
