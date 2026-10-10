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
public class MetroCsvParser {

    private static final String[] REQUIRED_COLUMNS = {"date", "station_id", "station_name", "entry_count", "exit_count"};

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
                // skip comment / watermark lines
                if (line.length > 0 && line[0].startsWith("#")) continue;

                if (headerLine == null) {
                    headerLine = line;
                    for (int i = 0; i < headerLine.length; i++) {
                        columnIndex.put(headerLine[i].trim().toLowerCase(), i);
                    }
                    for (String col : REQUIRED_COLUMNS) {
                        if (!columnIndex.containsKey(col)) {
                            errors.add("Missing required column: " + col);
                        }
                    }
                    if (!errors.isEmpty()) return new ParseResult<>(List.of(), 0, warnings, errors);
                    continue;
                }

                try {
                    LocalDate date = LocalDate.parse(col(line, columnIndex, "date"));
                    String stationId = col(line, columnIndex, "station_id");
                    String stationName = col(line, columnIndex, "station_name");
                    int entryCount = parsePositiveInt(col(line, columnIndex, "entry_count"), rowNum, "entry_count", warnings);
                    int exitCount = parsePositiveInt(col(line, columnIndex, "exit_count"), rowNum, "exit_count", warnings);

                    if (entryCount < 0 || exitCount < 0) {
                        warnings.add("Row " + rowNum + ": invalid numeric value — row excluded.");
                        excludedRows++;
                        continue;
                    }

                    boolean gateOk = true;
                    if (columnIndex.containsKey("gate_operational")) {
                        String v = col(line, columnIndex, "gate_operational").toLowerCase();
                        gateOk = !v.equals("false");
                    }

                    String notes = columnIndex.containsKey("notes") ? col(line, columnIndex, "notes") : "";
                    int total = entryCount + exitCount;

                    records.add(new DailyRidershipRecord(date, TransportSystem.METRO, stationId, stationName,
                            total, gateOk ? 1 : 0, null, false, notes));

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

    private int parsePositiveInt(String value, int rowNum, String colName, List<String> warnings) {
        try {
            int v = Integer.parseInt(value.replace(",", ""));
            if (v < 0) {
                warnings.add("Row " + rowNum + ": negative value in " + colName);
                return -1;
            }
            return v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void deduplicateByEntityId(List<DailyRidershipRecord> records, List<String> warnings) {
        Set<String> seen = new LinkedHashSet<>();
        List<DailyRidershipRecord> duplicates = new ArrayList<>();
        for (DailyRidershipRecord r : records) {
            String key = r.entityId() + ":" + r.date();
            if (!seen.add(key)) duplicates.add(r);
        }
        if (!duplicates.isEmpty()) {
            warnings.add("Duplicate station IDs found (" + duplicates.size() + "); last occurrence used.");
            // keep last occurrence for each key
            Map<String, DailyRidershipRecord> last = new LinkedHashMap<>();
            for (DailyRidershipRecord r : records) last.put(r.entityId() + ":" + r.date(), r);
            records.clear();
            records.addAll(last.values());
        }
    }
}
