package com.asims.pattern;

import com.asims.auth.Account;
import com.asims.auth.Permission;
import com.asims.model.Aircraft;
import com.asims.model.Fleet;
import com.asims.model.Incident;
import com.asims.model.Investigator;
import com.asims.model.Pilot;
import com.asims.repository.IncidentRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DESIGN PATTERN: FACADE (Structural)  +  the SUBJECT of the Observer pattern.
 *
 * Problem:  reporting an incident involves several steps — look up the pilot and the
 *           aircraft, ask the Factory to build the incident, store it in the Registry,
 *           save it to the database, then notify every Observer. The web server
 *           shouldn't have to know all that.
 * Solution: this class offers a few simple methods (reportIncident, escalateIncident, ...)
 *           and hides the steps behind them. The web server ONLY talks to this class.
 *
 * Every method that changes something receives the logged-in Account and first checks
 * that the account's role has the right Permission. So the rules "only an admin can
 * escalate" and "only an incident manager can close" live here, in one place.
 *
 * Viva answer: "The facade gives one simple front door to a complicated subsystem —
 *               the web layer calls reportIncident() and doesn't know a Factory,
 *               Registry, Repository or Observers even exist."
 */
public class IncidentService {

    private final IncidentRegistry registry;
    private final Fleet fleet;
    private final IncidentRepository repository;
    private final Map<String, Pilot> pilots = new LinkedHashMap<>();
    private final Map<String, Investigator> investigators = new LinkedHashMap<>();
    private final List<IncidentObserver> observers = new ArrayList<>();

    public IncidentService(IncidentRegistry registry, Fleet fleet, IncidentRepository repository) {
        this.registry = registry;
        this.fleet = fleet;
        this.repository = repository;
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

    /** Loads the saved incidents from the database into the registry (called once at start-up). */
    public void loadFromDatabase() {
        for (Incident incident : repository.loadAll(fleet, pilots, investigators)) {
            registry.add(incident);
        }
    }

    /**
     * Notifications are kept in memory, so after a restart the inboxes are empty even though
     * incidents are still waiting. This re-sends the events for incidents that are still
     * waiting: REPORTED ones (admin inbox) and ESCALATED ones (incident management inbox).
     */
    public void rebuildInboxes() {
        for (Incident incident : registry.getAll()) {
            if (incident.getStatus() == com.asims.model.IncidentStatus.REPORTED) {
                notifyObservers("REPORTED", incident);
            } else if (incident.getStatus() == com.asims.model.IncidentStatus.ESCALATED) {
                notifyObservers("ESCALATED", incident);
            }
        }
    }

    // ---------- Facade methods: the simple "front door" ----------

    /** PILOT: files a new incident. The reporting pilot is the logged-in pilot. */
    public Incident reportIncident(Account actor, String type, String flightNumber, String location,
                                   int severity, String aircraftRegistration, Map<String, String> extra) {
        requirePermission(actor, Permission.REPORT_INCIDENT);
        if (flightNumber == null || flightNumber.isBlank()) {
            throw new IllegalArgumentException("Flight number is required.");
        }
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Location is required.");
        }
        Pilot pilot = findPilot(actor.getStaffId());
        Aircraft aircraft = fleet.findAircraft(aircraftRegistration);
        String id = registry.generateId();

        Incident incident = IncidentFactory.create(type, id, flightNumber.trim(), location.trim(),
                severity, aircraft, pilot, extra);       // Factory
        registry.add(incident);                          // Singleton
        repository.save(incident);                       // Database
        notifyObservers("REPORTED", incident);           // Observer
        return incident;
    }

    /** ADMIN: passes a reported incident on to incident management. */
    public void escalateIncident(Account actor, String incidentId) {
        requirePermission(actor, Permission.ESCALATE_INCIDENT);
        Incident incident = registry.findById(incidentId);
        incident.escalate();
        repository.save(incident);
        notifyObservers("ESCALATED", incident);
    }

    /** INCIDENT MANAGER: starts the investigation of an escalated incident. */
    public void assignInvestigator(Account actor, String incidentId, String investigatorId) {
        requirePermission(actor, Permission.ASSIGN_INVESTIGATOR);
        Incident incident = registry.findById(incidentId);
        incident.assignInvestigator(findInvestigator(investigatorId));
        repository.save(incident);
        notifyObservers("ASSIGNED", incident);
    }

    /** PILOT (own incidents) or INCIDENT MANAGER: attaches evidence to an incident. */
    public void addEvidence(Account actor, String incidentId, String description) {
        requirePermission(actor, Permission.ADD_EVIDENCE);
        Incident incident = registry.findById(incidentId);
        if (!actor.getRole().canView(incident, actor)) {
            throw new SecurityException("You can only add evidence to incidents you can see.");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Evidence description is required.");
        }
        incident.addEvidence(description.trim(), actor.getFullName());
        repository.save(incident);
    }

    /** INCIDENT MANAGER: closes an incident that is under investigation. */
    public void closeIncident(Account actor, String incidentId) {
        requirePermission(actor, Permission.CLOSE_INCIDENT);
        Incident incident = registry.findById(incidentId);
        incident.close();
        repository.save(incident);
        notifyObservers("CLOSED", incident);
    }

    /** ADMIN: deletes the incident — its EvidenceLogs disappear with it (composition). */
    public void deleteIncident(Account actor, String incidentId) {
        requirePermission(actor, Permission.DELETE_INCIDENT);
        registry.findById(incidentId);          // fails with a clear message if it does not exist
        registry.remove(incidentId);
        repository.delete(incidentId);          // evidence rows are removed by ON DELETE CASCADE
    }

    // ---------- Read-only views for the web page ----------

    /** The incidents this account's role is allowed to see (newest first). */
    public List<Incident> getIncidentsFor(Account actor) {
        List<Incident> visible = new ArrayList<>();
        for (Incident incident : registry.getAll()) {
            if (actor.getRole().canView(incident, actor)) {
                visible.add(incident);
            }
        }
        java.util.Collections.reverse(visible);
        return visible;
    }

    public List<Investigator> getInvestigators() { return new ArrayList<>(investigators.values()); }
    public List<IncidentObserver> getObservers() { return List.copyOf(observers); }
    public Fleet getFleet() { return fleet; }

    // ---------- Helpers ----------

    private void requirePermission(Account actor, Permission permission) {
        if (actor == null || !actor.can(permission)) {
            String who = actor == null ? "Nobody" : actor.getRole().getLabel();
            throw new SecurityException(who + " accounts are not allowed to do this.");
        }
    }

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
