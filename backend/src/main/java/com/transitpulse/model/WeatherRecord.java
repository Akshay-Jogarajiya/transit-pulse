package com.transitpulse.model;

import java.time.LocalDate;

public record WeatherRecord(
        LocalDate date,
        String location,
        Double rainfallMm,
        String weatherCondition,
        Double maxTempC
) {}
