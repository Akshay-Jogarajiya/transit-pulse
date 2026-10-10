package com.transitpulse.ingestion;

import com.opencsv.CSVReader;
import com.transitpulse.model.IncidentRecord;
import com.transitpulse.model.Severity;
import com.transitpulse.model.TransportSystem;
import org.springframework.stereotype.Component;

import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class IncidentCsvParser {

    private static final String[] REQUIRED_COLUMNS = {"date", "system", "entity_id", "incident_type", "severity"};
    private static final Set<String> VALID_INCIDENT_TYPES = Set.of(
            "GATE_FAULT", "SIGNAL_FAULT", "VEHICLE_BREAKDOWN", "ROUTE_CANCELLATION", "INFRASTRUCTURE", "OTHER");

    public ParseResult<IncidentRecord> parse(Reader reader) {
        List<IncidentRecord> records = new ArrayList<>();
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
                    TransportSystem system = TransportSystem.valueOf(col(line, columnIndex, "system").toUpperCase());
                    String entityId = col(line, columnIndex, "entity_id");
                    String incidentType = col(line, columnIndex, "incident_type").toUpperCase();
                    if (!VALID_INCIDENT_TYPES.contains(incidentType)) {
                        warnings.add("Row " + rowNum + ": unknown incident_type '" + incidentType + "', mapped to OTHER.");
                        incidentType = "OTHER";
                    }
                    Severity severity = Severity.valueOf(col(line, columnIndex, "severity").toUpperCase());
                    String startTime = columnIndex.containsKey("start_time") ? col(line, columnIndex, "start_time") : "";
                    String endTime = columnIndex.containsKey("end_time") ? col(line, columnIndex, "end_time") : "";
                    String description = columnIndex.containsKey("description") ? col(line, columnIndex, "description") : "";

                    records.add(new IncidentRecord(date, system, entityId, incidentType, severity, startTime, endTime, description));

                } catch (DateTimeParseException e) {
                    warnings.add("Row " + rowNum + ": invalid date — row excluded.");
                    excludedRows++;
                } catch (IllegalArgumentException e) {
                    warnings.add("Row " + rowNum + ": invalid system or severity value — row excluded.");
                    excludedRows++;
                } catch (Exception e) {
                    warnings.add("Row " + rowNum + ": parse error (" + e.getMessage() + ") — row excluded.");
                    excludedRows++;
                }
            }
        } catch (Exception e) {
            errors.add("Failed to read CSV: " + e.getMessage());
        }

        return new ParseResult<>(records, excludedRows, warnings, errors);
    }

    private String col(String[] line, Map<String, Integer> idx, String name) {
        int i = idx.get(name);
        return i < line.length ? line[i].trim() : "";
    }
}
