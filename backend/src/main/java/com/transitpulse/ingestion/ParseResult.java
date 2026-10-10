package com.transitpulse.ingestion;

import java.util.List;

public record ParseResult<T>(
        List<T> records,
        int excludedRows,
        List<String> warnings,
        List<String> errors
) {
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
