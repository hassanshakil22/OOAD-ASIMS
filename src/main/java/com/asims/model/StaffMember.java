package com.asims.model;

/**
 * StaffMember — the ABSTRACT parent of every person using the system.
 *
 * ABSTRACTION:   you can't create a plain StaffMember, only a Pilot or Investigator.
 * ENCAPSULATION: id and name are private and read-only (no setters).
 * POLYMORPHISM:  each subclass answers getRole() differently.
 */
public abstract class StaffMember {

    private final String id;
    private final String name;

    protected StaffMember(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /** e.g. "Pilot (Captain)" or "Safety Investigator". */
    public abstract String getRole();

    public String getId() { return id; }
    public String getName() { return name; }
}
