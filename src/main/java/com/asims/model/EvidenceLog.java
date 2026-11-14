package com.asims.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One piece of evidence for an incident (photo, flight data, witness statement...).
 *
 * COMPOSITION: an EvidenceLog belongs to exactly one Incident.
 * The constructor is package-private (no "public"), so ONLY Incident.addEvidence()
 * can create one. When the incident is deleted, its evidence goes with it.
 */
public class EvidenceLog {

    private final String id;
    private final String description;
    private final String collectedBy;
    private final String collectedAt;

    EvidenceLog(String id, String description, String collectedBy) {
        this.id = id;
        this.description = description;
        this.collectedBy = collectedBy;
        this.collectedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public String getCollectedBy() { return collectedBy; }
    public String getCollectedAt() { return collectedAt; }
}
