package com.asims.pattern;

import com.asims.model.AirspaceIncident;
import com.asims.model.Aircraft;
import com.asims.model.Incident;
import com.asims.model.Pilot;
import com.asims.model.TechnicalIncident;
import com.asims.model.WeatherIncident;

import java.util.Map;

/**
 * DESIGN PATTERN: FACTORY (Creational)
 *
 * Problem:  the web page only sends a type name like "technical". The rest of the
 *           program shouldn't need to know which class to "new" for each type.
 * Solution: put all the "which subclass do I create?" logic in ONE place.
 *           Callers just say IncidentFactory.create("weather", ...) and get back an Incident.
 *
 * Viva answer: "Client code never calls new TechnicalIncident(...) directly — it asks
 *               the factory, so adding a new incident type only changes this one class."
 */
public class IncidentFactory {

    private IncidentFactory() {
        // only static methods — no objects needed
    }

    /**
     * @param type   "technical", "weather" or "airspace"
     * @param extra  the type-specific fields, e.g. {"aircraftSystem": "Engine", "grounded": "true"}
     */
    public static Incident create(String type, String id, String flightNumber, String location,
                                  int severity, Aircraft aircraft, Pilot reportedBy,
                                  Map<String, String> extra) {
        switch (type.toLowerCase()) {
            case "technical":
                return new TechnicalIncident(id, flightNumber, location, severity, aircraft, reportedBy,
                        extra.getOrDefault("aircraftSystem", "Unknown"),
                        Boolean.parseBoolean(extra.getOrDefault("grounded", "false")));
            case "weather":
                return new WeatherIncident(id, flightNumber, location, severity, aircraft, reportedBy,
                        extra.getOrDefault("weatherType", "Unknown"),
                        Integer.parseInt(extra.getOrDefault("turbulenceLevel", "1")));
            case "airspace":
                return new AirspaceIncident(id, flightNumber, location, severity, aircraft, reportedBy,
                        Double.parseDouble(extra.getOrDefault("separationNm", "5")));
            default:
                throw new IllegalArgumentException("Unknown incident type: " + type);
        }
    }
}
