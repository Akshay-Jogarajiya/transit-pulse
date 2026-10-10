package com.transitpulse.ai;

public record NarrativeResult(String text, String source) {
    public static NarrativeResult ai(String text) { return new NarrativeResult(text, "AI"); }
    public static NarrativeResult fallback(String text) { return new NarrativeResult(text, "FALLBACK"); }
}
