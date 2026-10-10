package com.transitpulse.model;

import java.time.LocalDate;

public record DailyRidershipRecord(
        LocalDate date,
        TransportSystem system,
        String entityId,
        String entityName,
        int totalPassengers,
        int operationalUnits,
        Integer scheduledUnits,
        boolean hasOpenIncident,
        String notes
) {}
