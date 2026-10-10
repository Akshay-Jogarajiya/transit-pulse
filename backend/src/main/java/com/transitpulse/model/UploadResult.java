package com.transitpulse.model;

import java.util.List;

public record UploadResult(
        String status,
        int rowCount,
        int excludedRows,
        List<String> warnings,
        List<String> errors
) {
    public static UploadResult success(int rowCount, int excludedRows, List<String> warnings) {
        return new UploadResult("OK", rowCount, excludedRows, warnings, List.of());
    }

    public static UploadResult failure(List<String> errors) {
        return new UploadResult("ERROR", 0, 0, List.of(), errors);
    }
}
