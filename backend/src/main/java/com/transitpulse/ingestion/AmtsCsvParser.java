package com.transitpulse.ingestion;

import com.opencsv.CSVReader;
import com.transitpulse.model.DailyRidershipRecord;
import com.transitpulse.model.TransportSystem;
import org.springframework.stereotype.Component;

import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class AmtsCsvParser {

    private static final String[] REQUIRED_COLUMNS = {"date", "depot_id", "depot_name", "route_id", "route_name", "passengers", "buses_operated"};

    public ParseResult<DailyRidershipRecord> parse(Reader reader) {
        List<DailyRidershipRecord> records = new ArrayList<>();
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
                    String depotId = col(line, columnIndex, "depot_id");
                    String depotName = col(line, columnIndex, "depot_name");
                    String routeId = col(line, columnIndex, "route_id");
                    String routeName = col(line, columnIndex, "route_name");
                    int passengers = parseInt(col(line, columnIndex, "passengers"));
                    int busesOperated = parseInt(col(line, columnIndex, "buses_operated"));

                    if (passengers < 0 || busesOperated < 0) {
                        warnings.add("Row " + rowNum + ": invalid value — row excluded.");
                        excludedRows++;
                        continue;
                    }

                    // Canonical entityId: depotId:routeId
                    String entityId = depotId + ":" + routeId;
                    String entityName = routeName + " (" + depotName + ")";
                    String notes = "";

                    records.add(new DailyRidershipRecord(date, TransportSystem.AMTS, entityId, entityName,
                            passengers, busesOperated, null, false, notes));

                } catch (DateTimeParseException e) {
                    warnings.add("Row " + rowNum + ": invalid date — row excluded.");
                    excludedRows++;
                } catch (Exception e) {
                    warnings.add("Row " + rowNum + ": parse error (" + e.getMessage() + ") — row excluded.");
                    excludedRows++;
                }
            }
        } catch (Exception e) {
            errors.add("Failed to read CSV: " + e.getMessage());
        }

        deduplicateByEntityId(records, warnings);
        return new ParseResult<>(records, excludedRows, warnings, errors);
    }

    private String col(String[] line, Map<String, Integer> idx, String name) {
        int i = idx.get(name);
        return i < line.length ? line[i].trim() : "";
    }

    private int parseInt(String v) {
        try { return Integer.parseInt(v.replace(",", "")); } catch (NumberFormatException e) { return -1; }
    }

    private void deduplicateByEntityId(List<DailyRidershipRecord> records, List<String> warnings) {
        Map<String, DailyRidershipRecord> last = new LinkedHashMap<>();
        int before = records.size();
        for (DailyRidershipRecord r : records) last.put(r.entityId() + ":" + r.date(), r);
        if (last.size() < before) {
            warnings.add("Duplicate AMTS route IDs found; last occurrence used.");
            records.clear();
            records.addAll(last.values());
        }
    }
}
