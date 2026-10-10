package com.transitpulse.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record BriefDto(
        LocalDate reportDate,
        LocalDateTime generatedAt,
        NetworkSummaryDto networkSummary,
        List<SystemSummaryDto> systemSummaries,
        List<ExceptionDto> exceptions,
        List<String> recommendedActions,
        String narrative,
        String narrativeSource,
        List<String> dataQualityWarnings,
        boolean isSyntheticData
) {}
