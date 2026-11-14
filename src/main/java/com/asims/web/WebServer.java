package com.asims.web;

import com.asims.model.Aircraft;
import com.asims.model.EvidenceLog;
import com.asims.model.Incident;
import com.asims.model.Investigator;
import com.asims.model.Pilot;
import com.asims.pattern.IncidentObserver;
import com.asims.pattern.IncidentService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The bridge between the browser and our Java objects.
 *
 * It only talks to IncidentService (the Facade) — it never touches the Registry,
 * Factory or Observers directly. It turns Java objects into JSON for the page.
 *
 * Endpoints:
 *   GET  /               the web page
 *   GET  /api/data       everything the page needs (incidents, staff, fleet, notifications)
 *   POST /api/report     report a new incident
 *   POST /api/assign     assign an investigator
 *   POST /api/evidence   add evidence to an incident
 *   POST /api/escalate   escalate an incident
 *   POST /api/close      close an incident
 *   POST /api/delete     delete an incident (shows composition)
 */
public class WebServer {

    private final IncidentService service;
    private final Gson gson = new Gson();

    public WebServer(IncidentService service) {
        this.service = service;
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", this::servePage);
        server.createContext("/api/data", exchange -> sendJson(exchange, 200, buildData()));

        server.createContext("/api/report", exchange -> handleAction(exchange, body -> {
            Map<String, String> extra = new LinkedHashMap<>(body);   // type-specific fields
            service.reportIncident(body.get("type"), body.get("flightNumber"), body.get("location"),
                    Integer.parseInt(body.get("severity")), body.get("aircraft"), body.get("pilotId"), extra);
        }));
        server.createContext("/api/assign", exchange -> handleAction(exchange, body ->
                service.assignInvestigator(body.get("incidentId"), body.get("investigatorId"))));
        server.createContext("/api/evidence", exchange -> handleAction(exchange, body ->
                service.addEvidence(body.get("incidentId"), body.get("description"), body.get("collectedBy"))));
        server.createContext("/api/escalate", exchange -> handleAction(exchange, body ->
                service.escalateIncident(body.get("incidentId"))));
        server.createContext("/api/close", exchange -> handleAction(exchange, body ->
                service.closeIncident(body.get("incidentId"))));
        server.createContext("/api/delete", exchange -> handleAction(exchange, body ->
                service.deleteIncident(body.get("incidentId"))));

        server.start();
        System.out.println("ASIMS is running at http://localhost:" + port);
    }

    // ---------- Request handling ----------

    /** A small interface so each endpoint can pass its action as a lambda. */
    private interface Action {
        void run(Map<String, String> body);
    }

    private void handleAction(HttpExchange exchange, Action action) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendJson(exchange, 405, Map.of("error", "Use POST"));
            return;
        }
        try {
            String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> body = gson.fromJson(json, new TypeToken<Map<String, String>>() { }.getType());
            action.run(body);
            sendJson(exchange, 200, Map.of("ok", true));
        } catch (RuntimeException e) {
            // e.g. invalid severity, closing without an investigator -> shown to the user
            sendJson(exchange, 400, Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    private void servePage(HttpExchange exchange) throws IOException {
        try (InputStream page = getClass().getResourceAsStream("/web/index.html")) {
            byte[] bytes = page.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }

    private void sendJson(HttpExchange exchange, int status, Object data) throws IOException {
        byte[] bytes = gson.toJson(data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    // ---------- Turning objects into simple maps for JSON ----------

    private Map<String, Object> buildData() {
        Map<String, Object> data = new LinkedHashMap<>();

        List<Map<String, Object>> incidents = new ArrayList<>();
        for (Incident incident : service.getAllIncidents()) {
            incidents.add(incidentToMap(incident));
        }
        data.put("incidents", incidents);

        List<Map<String, Object>> pilots = new ArrayList<>();
        for (Pilot pilot : service.getPilots()) {
            pilots.add(Map.of("id", pilot.getId(), "name", pilot.getName(), "role", pilot.getRole()));
        }
        data.put("pilots", pilots);

        List<Map<String, Object>> investigators = new ArrayList<>();
        for (Investigator inv : service.getInvestigators()) {
            investigators.add(Map.of("id", inv.getId(), "name", inv.getName(),
                    "specialization", inv.getSpecialization()));
        }
        data.put("investigators", investigators);

        List<Map<String, Object>> aircraft = new ArrayList<>();
        for (Aircraft a : service.getFleet().getAircraftList()) {
            aircraft.add(Map.of("registration", a.getRegistration(), "model", a.getModel()));
        }
        data.put("fleetName", service.getFleet().getAirlineName());
        data.put("aircraft", aircraft);

        List<Map<String, Object>> observers = new ArrayList<>();
        for (IncidentObserver observer : service.getObservers()) {
            observers.add(Map.of("name", observer.getName(), "notifications", observer.getNotifications()));
        }
        data.put("observers", observers);

        return data;
    }

    private Map<String, Object> incidentToMap(Incident incident) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", incident.getId());
        map.put("type", incident.getType());                     // polymorphic
        map.put("flightNumber", incident.getFlightNumber());
        map.put("location", incident.getLocation());
        map.put("reportedAt", incident.getReportedAt());
        map.put("severity", incident.getSeverity());
        map.put("riskScore", incident.calculateRiskScore());     // polymorphic
        map.put("riskLevel", incident.getRiskLevel());
        map.put("details", incident.getDetails());               // polymorphic
        map.put("status", incident.getStatus().name());
        map.put("aircraft", incident.getAircraft().getRegistration() + " " + incident.getAircraft().getModel());
        map.put("reportedBy", incident.getReportedBy().getName());
        Investigator inv = incident.getAssignedInvestigator();
        map.put("investigator", inv == null ? null : inv.getName());

        List<Map<String, Object>> evidence = new ArrayList<>();
        for (EvidenceLog log : incident.getEvidenceLogs()) {
            evidence.add(Map.of("id", log.getId(), "description", log.getDescription(),
                    "collectedBy", log.getCollectedBy(), "collectedAt", log.getCollectedAt()));
        }
        map.put("evidence", evidence);
        return map;
    }
}
