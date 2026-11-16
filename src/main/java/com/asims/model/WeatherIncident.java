package com.asims.model;

import java.util.Map;

/**
 * A weather-related hazard (turbulence, wind shear, thunderstorm, ...).
 *
 * INHERITANCE:  extends Incident.
 * POLYMORPHISM: risk depends mainly on how strong the turbulence was.
 */
public class WeatherIncident extends Incident {

    private final String weatherType;     // e.g. "Severe Turbulence"
    private final int turbulenceLevel;    // 1 (light) to 5 (extreme)

    public WeatherIncident(String id, String flightNumber, String location, int severity,
                           Aircraft aircraft, Pilot reportedBy,
                           String weatherType, int turbulenceLevel) {
        super(id, flightNumber, location, severity, aircraft, reportedBy);
        this.weatherType = weatherType;
        this.turbulenceLevel = turbulenceLevel;
    }

    /** Risk = severity + turbulence strength. */
    @Override
    public int calculateRiskScore() {
        int score = getSeverity() * 10 + turbulenceLevel * 10;
        return Math.min(score, 100);
    }

    @Override
    public String getType() {
        return "Weather";
    }

    @Override
    public String getDetails() {
        return weatherType + ", Turbulence level: " + turbulenceLevel + "/5";
    }

    @Override
    public Map<String, String> getExtraFields() {
        return Map.of("weatherType", weatherType, "turbulenceLevel", String.valueOf(turbulenceLevel));
    }

    public String getWeatherType() { return weatherType; }
    public int getTurbulenceLevel() { return turbulenceLevel; }
}
