package com.asims.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One piece of evidence for an incident (photo, flight data, witness statement...).
 *
 * COMPOSITION: an EvidenceLog belongs to exactly one Incident.
 * The constructors are package-private (no "public"), so ONLY Incident can create one
 * (through addEvidence() or restoreEvidence()). When the incident is deleted, its evidence
 * goes with it — in memory, and in the database too (ON DELETE CASCADE).
 */
public class EvidenceLog {

    private final String id;
    private final String description;
    private final String collectedBy;
    private final String collectedAt;

    /** New evidence: the time is "now". */
    EvidenceLog(String id, String description, String collectedBy) {
        this(id, description, collectedBy,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    /** Evidence loaded from the database: keeps its saved time. */
    EvidenceLog(String id, String description, String collectedBy, String collectedAt) {
        this.id = id;
        this.description = description;
        this.collectedBy = collectedBy;
        this.collectedAt = collectedAt;
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public String getCollectedBy() { return collectedBy; }
    public String getCollectedAt() { return collectedAt; }
}
