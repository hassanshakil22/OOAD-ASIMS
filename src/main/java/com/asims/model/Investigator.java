package com.asims.model;

/**
 * A safety investigator. Investigators are ASSIGNED to incidents
 * (association: Investigator "investigates" Incident).
 * INHERITANCE: extends StaffMember.
 */
public class Investigator extends StaffMember {

    private final String specialization;   // e.g. "Aircraft Systems"

    public Investigator(String id, String name, String specialization) {
        super(id, name);
        this.specialization = specialization;
    }

    @Override
    public String getRole() {
        return "Safety Investigator";
    }

    public String getSpecialization() { return specialization; }
}
