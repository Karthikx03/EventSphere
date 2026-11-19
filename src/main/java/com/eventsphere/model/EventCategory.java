package com.eventsphere.model;

public enum EventCategory {
    TECH("Tech Talk"),
    WORKSHOP("Workshop"),
    HACKATHON("Hackathon"),
    CULTURAL("Cultural"),
    MUSIC("Music"),
    SPORTS("Sports"),
    SEMINAR("Seminar");

    private final String displayName;

    EventCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
