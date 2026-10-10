package com.transitpulse.ingestion;

import com.transitpulse.model.DailyRidershipRecord;
import com.transitpulse.model.IncidentRecord;
import com.transitpulse.model.WeatherRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory session store for uploaded CSV data. Single-session, no persistence.
 */
@Service
public class UploadSessionService {

    private List<DailyRidershipRecord> metroToday = new ArrayList<>();
    private List<DailyRidershipRecord> metroBaseline = new ArrayList<>();
    private List<DailyRidershipRecord> brtsToday = new ArrayList<>();
    private List<DailyRidershipRecord> brtsBaseline = new ArrayList<>();
    private List<DailyRidershipRecord> amtsToday = new ArrayList<>();
    private List<DailyRidershipRecord> amtsBaseline = new ArrayList<>();
    private List<IncidentRecord> incidents = new ArrayList<>();
    private List<WeatherRecord> weather = new ArrayList<>();
    private final List<String> uploadWarnings = new ArrayList<>();

    // --- setters ---

    public void setMetroToday(List<DailyRidershipRecord> records, int excluded, List<String> warnings) {
        this.metroToday = new ArrayList<>(records);
        if (excluded > 0) uploadWarnings.add("Metro: " + excluded + " row(s) excluded — " + (warnings.isEmpty() ? "invalid data" : warnings.get(0)));
    }
    public void setMetroToday(List<DailyRidershipRecord> records) { this.metroToday = new ArrayList<>(records); }
    public void setMetroBaseline(List<DailyRidershipRecord> records) { this.metroBaseline = new ArrayList<>(records); }
    public void setBrtsToday(List<DailyRidershipRecord> records) { this.brtsToday = new ArrayList<>(records); }
    public void setBrtsBaseline(List<DailyRidershipRecord> records) { this.brtsBaseline = new ArrayList<>(records); }
    public void setAmtsToday(List<DailyRidershipRecord> records) { this.amtsToday = new ArrayList<>(records); }
    public void setAmtsBaseline(List<DailyRidershipRecord> records) { this.amtsBaseline = new ArrayList<>(records); }
    public void setIncidents(List<IncidentRecord> records) { this.incidents = new ArrayList<>(records); }
    public void setWeather(List<WeatherRecord> records) { this.weather = new ArrayList<>(records); }

    // --- getters ---

    public List<DailyRidershipRecord> getMetroToday() { return metroToday; }
    public List<DailyRidershipRecord> getMetroBaseline() { return metroBaseline; }
    public List<DailyRidershipRecord> getBrtsToday() { return brtsToday; }
    public List<DailyRidershipRecord> getBrtsBaseline() { return brtsBaseline; }
    public List<DailyRidershipRecord> getAmtsToday() { return amtsToday; }
    public List<DailyRidershipRecord> getAmtsBaseline() { return amtsBaseline; }
    public List<IncidentRecord> getIncidents() { return incidents; }
    public List<WeatherRecord> getWeather() { return weather; }

    public List<String> getUploadWarnings() { return new ArrayList<>(uploadWarnings); }

    public boolean hasTodayData() {
        return !metroToday.isEmpty() || !brtsToday.isEmpty() || !amtsToday.isEmpty();
    }
}
