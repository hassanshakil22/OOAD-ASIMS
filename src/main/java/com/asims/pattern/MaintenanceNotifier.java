package com.asims.pattern;

import com.asims.model.Incident;

import java.util.ArrayList;
import java.util.List;

/**
 * OBSERVER #2 — the maintenance team.
 * Only cares about incidents where the aircraft needs fixing.
 *
 * Note: it asks incident.requiresMaintenance() instead of checking
 * "is this a TechnicalIncident?". That's POLYMORPHISM — each incident type
 * decides for itself.
 */
public class MaintenanceNotifier implements IncidentObserver {

    private final List<String> notifications = new ArrayList<>();

    @Override
    public void update(String event, Incident incident) {
        if (event.equals("REPORTED") && incident.requiresMaintenance()) {
            notifications.add("Inspect aircraft " + incident.getAircraft().getRegistration()
                    + " (" + incident.getAircraft().getModel() + ") for " + incident.getId()
                    + ": " + incident.getDetails());
        }
    }

    @Override
    public String getName() {
        return "Maintenance Team";
    }

    @Override
    public List<String> getNotifications() {
        return List.copyOf(notifications);
    }
}
