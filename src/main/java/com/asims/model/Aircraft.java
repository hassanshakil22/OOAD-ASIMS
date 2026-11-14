package com.asims.model;

/**
 * A physical aeroplane. It exists on its own — it does not depend on a Fleet
 * or an Incident to live (see Fleet for AGGREGATION).
 */
public class Aircraft {

    private final String registration;   // e.g. "AP-BXK"
    private final String model;          // e.g. "Boeing 777-300ER"

    public Aircraft(String registration, String model) {
        this.registration = registration;
        this.model = model;
    }

    public String getRegistration() { return registration; }
    public String getModel() { return model; }
}
