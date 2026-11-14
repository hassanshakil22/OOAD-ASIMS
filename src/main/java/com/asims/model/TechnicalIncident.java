package com.asims.model;

/**
 * A mechanical or system failure on the aircraft (engine, hydraulics, ...).
 *
 * INHERITANCE:  extends Incident, so it gets id, flight number, evidence, etc. for free.
 * POLYMORPHISM: overrides calculateRiskScore() with its own formula.
 */
public class TechnicalIncident extends Incident {

    private final String aircraftSystem;   // e.g. "Hydraulics"
    private final boolean grounded;        // is the aircraft stuck on the ground?

    public TechnicalIncident(String id, String flightNumber, String location, int severity,
                             Aircraft aircraft, Pilot reportedBy,
                             String aircraftSystem, boolean grounded) {
        super(id, flightNumber, location, severity, aircraft, reportedBy);
        this.aircraftSystem = aircraftSystem;
        this.grounded = grounded;
    }

    /** Risk = severity, plus extra if the aircraft is grounded or a critical system failed. */
    @Override
    public int calculateRiskScore() {
        int score = getSeverity() * 12;
        if (grounded) score += 25;
        if (isCriticalSystem()) score += 15;
        return Math.min(score, 100);
    }

    private boolean isCriticalSystem() {
        return aircraftSystem.equalsIgnoreCase("Engine")
                || aircraftSystem.equalsIgnoreCase("Hydraulics")
                || aircraftSystem.equalsIgnoreCase("Flight Controls");
    }

    /** A grounded aircraft needs the maintenance team. */
    @Override
    public boolean requiresMaintenance() {
        return grounded;
    }

    @Override
    public String getType() {
        return "Technical";
    }

    @Override
    public String getDetails() {
        return "System: " + aircraftSystem + ", Grounded: " + (grounded ? "yes" : "no");
    }

    public String getAircraftSystem() { return aircraftSystem; }
    public boolean isGrounded() { return grounded; }
}
