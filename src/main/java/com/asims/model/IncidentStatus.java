package com.asims.model;

/**
 * The stages an incident goes through.
 * An enum means the status can ONLY be one of these four values — no typos possible.
 */
public enum IncidentStatus {
    REPORTED,
    UNDER_INVESTIGATION,
    ESCALATED,
    CLOSED
}
