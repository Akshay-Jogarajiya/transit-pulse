package com.transitpulse.analysis;

import com.transitpulse.model.ExceptionDto;
import com.transitpulse.model.NetworkSummaryDto;
import com.transitpulse.model.SystemSummaryDto;

import java.util.List;

public record AnalysisResult(
        NetworkSummaryDto networkSummary,
        List<SystemSummaryDto> systemSummaries,
        List<ExceptionDto> topExceptions,
        int totalExceptionsFound,
        List<String> dataQualityWarnings
) {}
