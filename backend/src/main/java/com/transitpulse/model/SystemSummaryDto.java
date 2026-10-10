package com.transitpulse.model;

public record SystemSummaryDto(
        TransportSystem system,
        int totalToday,
        int totalYesterday,
        Double pctChange,
        Double scheduleAdherence
) {}
