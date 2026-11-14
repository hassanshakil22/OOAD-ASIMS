package com.asims.model;

/**
 * A pilot. Pilots REPORT incidents (association: Pilot "reports" Incident).
 * INHERITANCE: extends StaffMember.
 */
public class Pilot extends StaffMember {

    private final String rank;   // e.g. "Captain", "First Officer"

    public Pilot(String id, String name, String rank) {
        super(id, name);
        this.rank = rank;
    }

    @Override
    public String getRole() {
        return "Pilot (" + rank + ")";
    }

    public String getRank() { return rank; }
}
