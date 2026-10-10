package com.transitpulse.ingestion;

import com.opencsv.CSVReader;
import com.transitpulse.model.WeatherRecord;
import org.springframework.stereotype.Component;

import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class WeatherCsvParser {

    private static final String[] REQUIRED_COLUMNS = {"date"};

    public ParseResult<WeatherRecord> parse(Reader reader) {
        List<WeatherRecord> records = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int excludedRows = 0;
        Map<String, Integer> columnIndex = new HashMap<>();

        try (CSVReader csv = new CSVReader(reader)) {
            String[] headerLine = null;
            String[] line;
            int rowNum = 0;

            while ((line = csv.readNext()) != null) {
                rowNum++;
                if (line.length > 0 && line[0].startsWith("#")) continue;

                if (headerLine == null) {
                    headerLine = line;
                    for (int i = 0; i < headerLine.length; i++) columnIndex.put(headerLine[i].trim().toLowerCase(), i);
                    for (String col : REQUIRED_COLUMNS) {
                        if (!columnIndex.containsKey(col)) errors.add("Missing required column: " + col);
                    }
                    if (!errors.isEmpty()) return new ParseResult<>(List.of(), 0, warnings, errors);
                    continue;
                }

                try {
                    LocalDate date = LocalDate.parse(col(line, columnIndex, "date"));
                    String location = columnIndex.containsKey("location") ? col(line, columnIndex, "location") : "Ahmedabad";
                    Double rainfall = columnIndex.containsKey("rainfall_mm") ? parseDouble(col(line, columnIndex, "rainfall_mm")) : null;
                    String condition = columnIndex.containsKey("weather_condition") ? col(line, columnIndex, "weather_condition") : null;
                    Double temp = columnIndex.containsKey("max_temp_c") ? parseDouble(col(line, columnIndex, "max_temp_c")) : null;

                    records.add(new WeatherRecord(date, location, rainfall, condition, temp));

                } catch (DateTimeParseException e) {
                    warnings.add("Row " + rowNum + ": invalid date — row excluded.");
                    excludedRows++;
                } catch (Exception e) {
                    warnings.add("Row " + rowNum + ": parse error — row excluded.");
                    excludedRows++;
                }
            }
        } catch (Exception e) {
            errors.add("Failed to read CSV: " + e.getMessage());
        }

        return new ParseResult<>(records, excludedRows, warnings, errors);
    }

    private String col(String[] line, Map<String, Integer> idx, String name) {
        Integer i = idx.get(name);
        return (i != null && i < line.length) ? line[i].trim() : "";
    }

    private Double parseDouble(String v) {
        try { return v.isBlank() ? null : Double.parseDouble(v); } catch (NumberFormatException e) { return null; }
    }
}
