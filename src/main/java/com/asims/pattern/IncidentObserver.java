package com.asims.pattern;

import com.asims.model.Incident;

import java.util.List;

/**
 * DESIGN PATTERN: OBSERVER (Behavioral) — the observer interface.
 *
 * Problem:  when an incident is reported or escalated, several parties must be told
 *           (the aviation authority, the maintenance team...). We don't want
 *           IncidentService to contain "call authority, call maintenance, call ..." code.
 * Solution: anyone interested implements this small interface and subscribes.
 *           IncidentService (the "subject") just loops over its observers and calls update().
 *
 * Viva answer: "The service doesn't know who is listening. To add a new listener we write
 *               a new class implementing IncidentObserver — no change to the service."
 */
public interface IncidentObserver {

    /** Called by the subject whenever something happens, e.g. event = "REPORTED". */
    void update(String event, Incident incident);

    /** Display name, e.g. "Aviation Authority". */
    String getName();

    /** Messages this observer has received so far (shown on the web page). */
    List<String> getNotifications();
}
