package com.asims.pattern;

import com.asims.model.Incident;

import java.util.ArrayList;
import java.util.List;

/**
 * OBSERVER #1 — the national aviation authority.
 * Wants to hear about every new incident and every escalation.
 */
public class AuthorityNotifier implements IncidentObserver {

    private final List<String> notifications = new ArrayList<>();

    @Override
    public void update(String event, Incident incident) {
        if (event.equals("REPORTED") || event.equals("ESCALATED")) {
            notifications.add(event + ": " + incident.getId() + " (" + incident.getType()
                    + ", flight " + incident.getFlightNumber()
                    + ") risk " + incident.calculateRiskScore() + " " + incident.getRiskLevel());
        }
    }

    @Override
    public String getName() {
        return "Aviation Authority";
    }

    @Override
    public List<String> getNotifications() {
        return List.copyOf(notifications);
    }
}
