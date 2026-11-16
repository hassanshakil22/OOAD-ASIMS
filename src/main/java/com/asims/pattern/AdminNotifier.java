package com.asims.pattern;

import com.asims.model.Incident;

import java.util.ArrayList;
import java.util.List;

/**
 * OBSERVER #3 — the admin's inbox.
 * Every newly reported incident lands here, waiting for the admin to review and escalate it.
 *
 * Adding this class needed NO change to IncidentService (Open/Closed principle):
 * it just implements IncidentObserver and is subscribed in Main.
 */
public class AdminNotifier implements IncidentObserver {

    private final List<String> notifications = new ArrayList<>();

    @Override
    public void update(String event, Incident incident) {
        if (event.equals("REPORTED")) {
            notifications.add("New report " + incident.getId() + " from " + incident.getReportedBy().getName()
                    + " (" + incident.getType() + ", flight " + incident.getFlightNumber()
                    + ", risk " + incident.calculateRiskScore() + " " + incident.getRiskLevel()
                    + ") is waiting for review.");
        }
    }

    @Override
    public String getName() {
        return "Admin Inbox";
    }

    @Override
    public List<String> getNotifications() {
        return List.copyOf(notifications);
    }
}
