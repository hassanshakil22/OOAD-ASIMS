package com.asims.auth;

/** Things a user may be allowed to do. Each Role is given a set of these. */
public enum Permission {
    REPORT_INCIDENT,        // pilots file new incidents
    ADD_EVIDENCE,           // attach evidence to an incident
    ESCALATE_INCIDENT,      // admin passes an incident on to incident management
    DELETE_INCIDENT,        // admin removes an incident (and its evidence)
    ASSIGN_INVESTIGATOR,    // incident manager starts the investigation
    CLOSE_INCIDENT          // incident manager closes the case
}
