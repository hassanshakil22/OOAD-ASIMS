package com.asims.pattern;

import com.asims.model.Incident;

import java.util.ArrayList;
import java.util.List;

/**
 * OBSERVER #4 — the incident management team.
 * Only hears about incidents that the admin has ESCALATED.
 */
public class IncidentManagementNotifier implements IncidentObserver {

    private final List<String> notifications = new ArrayList<>();

    @Override
    public void update(String event, Incident incident) {
        if (event.equals("ESCALATED")) {
            notifications.add("Escalated " + incident.getId() + " (" + incident.getType() + ", flight "
                    + incident.getFlightNumber() + ", risk " + incident.calculateRiskScore() + " "
                    + incident.getRiskLevel() + "): assign an investigator.");
        }
    }

    @Override
    public String getName() {
        return "Incident Management";
    }

    @Override
    public List<String> getNotifications() {
        return List.copyOf(notifications);
    }
}
