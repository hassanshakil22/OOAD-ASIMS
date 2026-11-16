package com.asims.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Incident — the ABSTRACT parent of every kind of safety incident.
 *
 * OOP concepts shown here:
 *  - ABSTRACTION:   this class is abstract, so you can't create a plain "Incident".
 *                   You must create a TechnicalIncident, WeatherIncident or AirspaceIncident.
 *  - ENCAPSULATION: every field is private. Other classes must use the public methods,
 *                   e.g. setSeverity() refuses values outside 1–5, and the status can only
 *                   change through escalate() / assignInvestigator() / close(), which enforce
 *                   the workflow  REPORTED -> ESCALATED -> UNDER_INVESTIGATION -> CLOSED.
 *  - POLYMORPHISM:  calculateRiskScore(), getType(), getDetails() and getExtraFields() are
 *                   abstract — each subclass gives its own version.
 *  - COMPOSITION:   the incident creates and owns its EvidenceLog objects.
 *  - ASSOCIATION:   the incident refers to an Aircraft, the Pilot who reported it,
 *                   and (optionally) an assigned Investigator.
 */
public abstract class Incident {

    private final String id;
    private final String flightNumber;
    private final String location;
    private String reportedAt;                   // only changed when loading from the database
    private int severity;                        // 1 (minor) to 5 (catastrophic)
    private IncidentStatus status;

    private final Aircraft aircraft;             // association
    private final Pilot reportedBy;              // association
    private Investigator assignedInvestigator;   // association (may be null)

    private final List<EvidenceLog> evidenceLogs = new ArrayList<>();  // composition

    protected Incident(String id, String flightNumber, String location, int severity,
                       Aircraft aircraft, Pilot reportedBy) {
        this.id = id;
        this.flightNumber = flightNumber;
        this.location = location;
        this.aircraft = aircraft;
        this.reportedBy = reportedBy;
        this.reportedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        this.status = IncidentStatus.REPORTED;
        setSeverity(severity);   // goes through the validation below
    }

    // ---------- Abstract methods: every subclass MUST implement these ----------

    /** Risk score from 0 to 100. Each incident type calculates it differently. */
    public abstract int calculateRiskScore();

    /** Short name of the incident type, e.g. "Technical". */
    public abstract String getType();

    /** Type-specific details shown on the web page, e.g. "System: Hydraulics, Grounded: yes". */
    public abstract String getDetails();

    /**
     * The type-specific fields as text, using the same keys that IncidentFactory reads
     * (e.g. "aircraftSystem", "grounded"). The database stores this, and the factory
     * rebuilds the right subclass from it when the data is loaded again.
     */
    public abstract Map<String, String> getExtraFields();

    /** Does this incident need the maintenance team? Most don't; subclasses can override. */
    public boolean requiresMaintenance() {
        return false;
    }

    // ---------- Behaviour ----------

    /** Turns the numeric risk score into a label. Works for every subclass. */
    public String getRiskLevel() {
        int score = calculateRiskScore();
        if (score >= 80) return "CRITICAL";
        if (score >= 60) return "HIGH";
        if (score >= 40) return "MEDIUM";
        return "LOW";
    }

    /** COMPOSITION: the incident itself creates the EvidenceLog — no one else can. */
    public EvidenceLog addEvidence(String description, String collectedBy) {
        String evidenceId = id + "-E" + (evidenceLogs.size() + 1);
        EvidenceLog log = new EvidenceLog(evidenceId, description, collectedBy);
        evidenceLogs.add(log);
        return log;
    }

    /** Admin step: REPORTED -> ESCALATED. */
    public void escalate() {
        if (status != IncidentStatus.REPORTED) {
            throw new IllegalStateException("Only a REPORTED incident can be escalated (current status: " + status + ").");
        }
        this.status = IncidentStatus.ESCALATED;
    }

    /** Incident manager step: ESCALATED -> UNDER_INVESTIGATION. */
    public void assignInvestigator(Investigator investigator) {
        if (status != IncidentStatus.ESCALATED && status != IncidentStatus.UNDER_INVESTIGATION) {
            throw new IllegalStateException(
                    "An investigator can only be assigned after the incident is escalated (current status: " + status + ").");
        }
        this.assignedInvestigator = investigator;
        this.status = IncidentStatus.UNDER_INVESTIGATION;
    }

    /** Incident manager step: UNDER_INVESTIGATION -> CLOSED. */
    public void close() {
        if (status != IncidentStatus.UNDER_INVESTIGATION) {
            throw new IllegalStateException(
                    "Only an incident that is under investigation can be closed (current status: " + status + ").");
        }
        this.status = IncidentStatus.CLOSED;
    }

    // ---------- Used only when loading saved data from the database ----------

    /** Puts back the saved timestamp, status and investigator (skips the workflow checks). */
    public void restoreState(String reportedAt, IncidentStatus status, Investigator investigator) {
        this.reportedAt = reportedAt;
        this.status = status;
        this.assignedInvestigator = investigator;
    }

    /** Puts back a saved evidence log. The incident still creates it itself (composition). */
    public void restoreEvidence(String evidenceId, String description, String collectedBy, String collectedAt) {
        evidenceLogs.add(new EvidenceLog(evidenceId, description, collectedBy, collectedAt));
    }

    // ---------- Getters and setters (ENCAPSULATION) ----------

    public void setSeverity(int severity) {
        if (severity < 1 || severity > 5) {
            throw new IllegalArgumentException("Severity must be between 1 and 5, got " + severity);
        }
        this.severity = severity;
    }

    public String getId() { return id; }
    public String getFlightNumber() { return flightNumber; }
    public String getLocation() { return location; }
    public String getReportedAt() { return reportedAt; }
    public int getSeverity() { return severity; }
    public IncidentStatus getStatus() { return status; }
    public Aircraft getAircraft() { return aircraft; }
    public Pilot getReportedBy() { return reportedBy; }
    public Investigator getAssignedInvestigator() { return assignedInvestigator; }

    /** Returns a read-only copy so outside code can't add/remove evidence directly. */
    public List<EvidenceLog> getEvidenceLogs() {
        return List.copyOf(evidenceLogs);
    }
}
