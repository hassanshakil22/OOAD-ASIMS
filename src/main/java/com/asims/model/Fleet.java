package com.asims.model;

import java.util.ArrayList;
import java.util.List;

/**
 * An airline's fleet — a collection of aircraft.
 *
 * AGGREGATION ("has-a", weak): the Aircraft objects are created OUTSIDE the Fleet
 * and then added to it. If the Fleet is deleted, the aircraft still exist.
 * Compare with Incident -> EvidenceLog, which is COMPOSITION (strong).
 */
public class Fleet {

    private final String airlineName;
    private final List<Aircraft> aircraftList = new ArrayList<>();

    public Fleet(String airlineName) {
        this.airlineName = airlineName;
    }

    public void addAircraft(Aircraft aircraft) {
        aircraftList.add(aircraft);
    }

    /** Removes the aircraft from the fleet — the Aircraft object itself still exists. */
    public void removeAircraft(Aircraft aircraft) {
        aircraftList.remove(aircraft);
    }

    public Aircraft findAircraft(String registration) {
        for (Aircraft aircraft : aircraftList) {
            if (aircraft.getRegistration().equals(registration)) {
                return aircraft;
            }
        }
        throw new IllegalArgumentException("No aircraft with registration " + registration);
    }

    public String getAirlineName() { return airlineName; }

    public List<Aircraft> getAircraftList() {
        return List.copyOf(aircraftList);
    }
}
