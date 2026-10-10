package com.transitpulse.model;

import java.util.List;

public record ExceptionDto(
        String entityId,
        String entityName,
        TransportSystem system,
        String metric,
        int valueToday,
        int valueBaseline,
        Double pctChange,
        Severity severity,
        List<EvidenceItem> evidence
) {}
