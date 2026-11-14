package com.asims.pattern;

import com.asims.model.Aircraft;
import com.asims.model.Fleet;
import com.asims.model.Incident;
import com.asims.model.Investigator;
import com.asims.model.Pilot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DESIGN PATTERN: FACADE (Structural)  +  the SUBJECT of the Observer pattern.
 *
 * Problem:  reporting an incident involves several steps — look up the pilot and the
 *           aircraft, ask the Factory to build the incident, store it in the Registry,
 *           then notify every Observer. The web server shouldn't have to know all that.
 * Solution: this class offers a few simple methods (reportIncident, escalateIncident, ...)
 *           and hides the steps behind them. The web server ONLY talks to this class.
 *
 * Viva answer: "The facade gives one simple front door to a complicated subsystem —
 *               the web layer calls reportIncident() and doesn't know a Factory,
 *               Registry or Observers even exist."
 */
public class IncidentService {

    private final IncidentRegistry registry;
    private final Fleet fleet;
    private final Map<String, Pilot> pilots = new LinkedHashMap<>();
    private final Map<String, Investigator> investigators = new LinkedHashMap<>();
    private final List<IncidentObserver> observers = new ArrayList<>();

    public IncidentService(IncidentRegistry registry, Fleet fleet) {
        this.registry = registry;
        this.fleet = fleet;
    }

    // ---------- Observer pattern: subject methods ----------

    public void addObserver(IncidentObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers(String event, Incident incident) {
        for (IncidentObserver observer : observers) {
            observer.update(event, incident);
        }
    }

    // ---------- Setup ----------

    public void addPilot(Pilot pilot) {
        pilots.put(pilot.getId(), pilot);
    }

    public void addInvestigator(Investigator investigator) {
        investigators.put(investigator.getId(), investigator);
    }

    // ---------- Facade methods: the simple "front door" ----------

    public Incident reportIncident(String type, String flightNumber, String location, int severity,
                                   String aircraftRegistration, String pilotId,
                                   Map<String, String> extra) {
        Pilot pilot = findPilot(pilotId);
        Aircraft aircraft = fleet.findAircraft(aircraftRegistration);
        String id = registry.generateId();

        Incident incident = IncidentFactory.create(type, id, flightNumber, location,
                severity, aircraft, pilot, extra);       // Factory
        registry.add(incident);                          // Singleton
        notifyObservers("REPORTED", incident);           // Observer
        return incident;
    }

    public void assignInvestigator(String incidentId, String investigatorId) {
        Incident incident = registry.findById(incidentId);
        incident.assignInvestigator(findInvestigator(investigatorId));
        notifyObservers("ASSIGNED", incident);
    }

    public void addEvidence(String incidentId, String description, String collectedBy) {
        registry.findById(incidentId).addEvidence(description, collectedBy);
    }

    public void escalateIncident(String incidentId) {
        Incident incident = registry.findById(incidentId);
        incident.escalate();
        notifyObservers("ESCALATED", incident);
    }

    public void closeIncident(String incidentId) {
        Incident incident = registry.findById(incidentId);
        incident.close();
        notifyObservers("CLOSED", incident);
    }

    /** Deletes the incident — its EvidenceLogs disappear with it (composition). */
    public void deleteIncident(String incidentId) {
        registry.remove(incidentId);
    }

    // ---------- Read-only views for the web page ----------

    public List<Incident> getAllIncidents() { return registry.getAll(); }
    public List<Pilot> getPilots() { return new ArrayList<>(pilots.values()); }
    public List<Investigator> getInvestigators() { return new ArrayList<>(investigators.values()); }
    public List<IncidentObserver> getObservers() { return List.copyOf(observers); }
    public Fleet getFleet() { return fleet; }

    // ---------- Helpers ----------

    private Pilot findPilot(String id) {
        Pilot pilot = pilots.get(id);
        if (pilot == null) throw new IllegalArgumentException("No pilot with id " + id);
        return pilot;
    }

    private Investigator findInvestigator(String id) {
        Investigator investigator = investigators.get(id);
        if (investigator == null) throw new IllegalArgumentException("No investigator with id " + id);
        return investigator;
    }
}
