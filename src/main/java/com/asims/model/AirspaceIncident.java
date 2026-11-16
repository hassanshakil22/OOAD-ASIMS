package com.asims.model;

import java.util.Map;

/**
 * Two aircraft came closer than allowed (a "loss of separation").
 *
 * INHERITANCE:  extends Incident.
 * POLYMORPHISM: the closer the aircraft got, the higher the risk.
 */
public class AirspaceIncident extends Incident {

    private final double separationNm;    // closest distance between the aircraft, in nautical miles

    public AirspaceIncident(String id, String flightNumber, String location, int severity,
                            Aircraft aircraft, Pilot reportedBy,
                            double separationNm) {
        super(id, flightNumber, location, severity, aircraft, reportedBy);
        this.separationNm = separationNm;
    }

    /** Risk = severity + a bonus that grows as the separation shrinks. */
    @Override
    public int calculateRiskScore() {
        int score = getSeverity() * 10;
        if (separationNm < 1.0) {
            score += 50;          // extremely close
        } else if (separationNm < 3.0) {
            score += 30;          // too close (standard minimum is about 3–5 nm)
        } else {
            score += 10;
        }
        return Math.min(score, 100);
    }

    @Override
    public String getType() {
        return "Airspace";
    }

    @Override
    public String getDetails() {
        return "Closest separation: " + separationNm + " nm";
    }

    @Override
    public Map<String, String> getExtraFields() {
        return Map.of("separationNm", String.valueOf(separationNm));
    }

    public double getSeparationNm() { return separationNm; }
}
