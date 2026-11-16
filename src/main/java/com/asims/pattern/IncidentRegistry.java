package com.asims.pattern;

import com.asims.model.Incident;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DESIGN PATTERN: SINGLETON (Creational)
 *
 * Problem:  the whole app must share ONE list of incidents. If two parts of the
 *           program each had their own list, they would disagree.
 * Solution: make the constructor private (nobody can call "new IncidentRegistry()")
 *           and hand out the one and only instance through getInstance().
 *
 * Viva answer: "The constructor is private, and getInstance() always returns the
 *               same static object, so there can only ever be one registry."
 */
public class IncidentRegistry {

    private static IncidentRegistry instance;   // the single shared object

    private final Map<String, Incident> incidents = new LinkedHashMap<>();
    private int nextNumber = 101;

    private IncidentRegistry() {
    }

    /** "synchronized" makes it safe if two threads call this at the same time. */
    public static synchronized IncidentRegistry getInstance() {
        if (instance == null) {
            instance = new IncidentRegistry();
        }
        return instance;
    }

    public String generateId() {
        return "INC-" + (nextNumber++);
    }

    public void add(Incident incident) {
        incidents.put(incident.getId(), incident);
        // Incidents loaded from the database already own ids like INC-107:
        // make sure the next generated id does not clash with them.
        String number = incident.getId().replaceAll("\\D", "");
        if (!number.isEmpty()) {
            nextNumber = Math.max(nextNumber, Integer.parseInt(number) + 1);
        }
    }

    public Incident findById(String id) {
        Incident incident = incidents.get(id);
        if (incident == null) {
            throw new IllegalArgumentException("No incident with id " + id);
        }
        return incident;
    }

    /** Removing the incident also removes its EvidenceLogs (composition). */
    public void remove(String id) {
        incidents.remove(id);
    }

    public List<Incident> getAll() {
        return new ArrayList<>(incidents.values());
    }

    /** Used by the tests so each test starts with an empty registry. */
    public void clear() {
        incidents.clear();
        nextNumber = 101;
    }
}
