package com.transitpulse.model;

public record NetworkSummaryDto(
        int totalToday,
        int totalYesterday,
        Double pctChange
) {}
