package com.asims.auth;

import com.asims.model.Incident;
import com.asims.model.IncidentStatus;

import java.util.EnumSet;
import java.util.Set;

/**
 * The three kinds of user, each with their own portal.
 *
 * POLYMORPHISM: every constant overrides canView(), so "which incidents may this
 * user see?" is answered by the role itself — no if/else chain anywhere else.
 *
 *   PILOT             reports incidents and sees only their own
 *   ADMIN             receives every reported incident and escalates it
 *   INCIDENT_MANAGER  receives escalated incidents, assigns investigators, closes cases
 */
public enum Role {

    PILOT("Pilot", "/pilot", EnumSet.of(Permission.REPORT_INCIDENT, Permission.ADD_EVIDENCE)) {
        @Override
        public boolean canView(Incident incident, Account user) {
            return incident.getReportedBy().getId().equals(user.getStaffId());
        }
    },

    ADMIN("Admin", "/admin", EnumSet.of(Permission.ESCALATE_INCIDENT, Permission.DELETE_INCIDENT)) {
        @Override
        public boolean canView(Incident incident, Account user) {
            return true;
        }
    },

    INCIDENT_MANAGER("Incident Manager", "/manager",
            EnumSet.of(Permission.ADD_EVIDENCE, Permission.ASSIGN_INVESTIGATOR, Permission.CLOSE_INCIDENT)) {
        @Override
        public boolean canView(Incident incident, Account user) {
            // only incidents that the admin has already escalated
            return incident.getStatus() != IncidentStatus.REPORTED;
        }
    };

    private final String label;
    private final String homePath;
    private final Set<Permission> permissions;

    Role(String label, String homePath, Set<Permission> permissions) {
        this.label = label;
        this.homePath = homePath;
        this.permissions = permissions;
    }

    /** Which incidents may a user with this role see? */
    public abstract boolean canView(Incident incident, Account user);

    public boolean has(Permission permission) { return permissions.contains(permission); }
    public String getLabel() { return label; }
    public String getHomePath() { return homePath; }
}
