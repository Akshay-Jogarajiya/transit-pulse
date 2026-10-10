package com.transitpulse.ingestion;

import com.transitpulse.model.UploadResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/upload")
public class IngestionController {

    private final MetroCsvParser metroParser;
    private final BrtsCsvParser brtsParser;
    private final AmtsCsvParser amtsParser;
    private final IncidentCsvParser incidentParser;
    private final WeatherCsvParser weatherParser;
    private final UploadSessionService session;

    public IngestionController(MetroCsvParser metroParser, BrtsCsvParser brtsParser,
                               AmtsCsvParser amtsParser, IncidentCsvParser incidentParser,
                               WeatherCsvParser weatherParser, UploadSessionService session) {
        this.metroParser = metroParser;
        this.brtsParser = brtsParser;
        this.amtsParser = amtsParser;
        this.incidentParser = incidentParser;
        this.weatherParser = weatherParser;
        this.session = session;
    }

    @PostMapping("/metro")
    public ResponseEntity<UploadResult> uploadMetro(@RequestParam("file") MultipartFile file,
                                                     @RequestParam(value = "baseline", defaultValue = "false") boolean baseline) {
        try {
            var result = metroParser.parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
            if (result.hasErrors()) return ResponseEntity.badRequest().body(UploadResult.failure(result.errors()));
            if (baseline) session.setMetroBaseline(result.records());
            else session.setMetroToday(result.records(), result.excludedRows(), result.warnings());
            return ResponseEntity.ok(UploadResult.success(result.records().size(), result.excludedRows(), result.warnings()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(UploadResult.failure(java.util.List.of("Server error: " + e.getMessage())));
        }
    }

    @PostMapping("/brts")
    public ResponseEntity<UploadResult> uploadBrts(@RequestParam("file") MultipartFile file,
                                                     @RequestParam(value = "baseline", defaultValue = "false") boolean baseline) {
        try {
            var result = brtsParser.parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
            if (result.hasErrors()) return ResponseEntity.badRequest().body(UploadResult.failure(result.errors()));
            if (baseline) session.setBrtsBaseline(result.records());
            else session.setBrtsToday(result.records());
            return ResponseEntity.ok(UploadResult.success(result.records().size(), result.excludedRows(), result.warnings()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(UploadResult.failure(java.util.List.of("Server error: " + e.getMessage())));
        }
    }

    @PostMapping("/amts")
    public ResponseEntity<UploadResult> uploadAmts(@RequestParam("file") MultipartFile file,
                                                     @RequestParam(value = "baseline", defaultValue = "false") boolean baseline) {
        try {
            var result = amtsParser.parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
            if (result.hasErrors()) return ResponseEntity.badRequest().body(UploadResult.failure(result.errors()));
            if (baseline) session.setAmtsBaseline(result.records());
            else session.setAmtsToday(result.records());
            return ResponseEntity.ok(UploadResult.success(result.records().size(), result.excludedRows(), result.warnings()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(UploadResult.failure(java.util.List.of("Server error: " + e.getMessage())));
        }
    }

    @PostMapping("/incidents")
    public ResponseEntity<UploadResult> uploadIncidents(@RequestParam("file") MultipartFile file) {
        try {
            var result = incidentParser.parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
            if (result.hasErrors()) return ResponseEntity.badRequest().body(UploadResult.failure(result.errors()));
            session.setIncidents(result.records());
            return ResponseEntity.ok(UploadResult.success(result.records().size(), result.excludedRows(), result.warnings()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(UploadResult.failure(java.util.List.of("Server error: " + e.getMessage())));
        }
    }

    @PostMapping("/weather")
    public ResponseEntity<UploadResult> uploadWeather(@RequestParam("file") MultipartFile file) {
        try {
            var result = weatherParser.parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
            if (result.hasErrors()) return ResponseEntity.badRequest().body(UploadResult.failure(result.errors()));
            session.setWeather(result.records());
            return ResponseEntity.ok(UploadResult.success(result.records().size(), result.excludedRows(), result.warnings()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(UploadResult.failure(java.util.List.of("Server error: " + e.getMessage())));
        }
    }
}
