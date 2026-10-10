package com.transitpulse.model;

import java.time.LocalDate;

public record IncidentRecord(
        LocalDate date,
        TransportSystem system,
        String entityId,
        String incidentType,
        Severity severity,
        String startTime,
        String endTime,
        String description
) {}
