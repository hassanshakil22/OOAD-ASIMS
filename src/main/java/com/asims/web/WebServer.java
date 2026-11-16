package com.asims.web;

import com.asims.auth.Account;
import com.asims.auth.AuthService;
import com.asims.auth.Role;
import com.asims.model.Aircraft;
import com.asims.model.EvidenceLog;
import com.asims.model.Incident;
import com.asims.model.Investigator;
import com.asims.pattern.IncidentObserver;
import com.asims.pattern.IncidentService;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The bridge between the browser and our Java objects.
 *
 * It only talks to IncidentService (the Facade) and AuthService — never to the
 * registry, factory or database directly.
 *
 * Pages (each one needs the matching role, otherwise the user is sent to the login page):
 *   /login     login form                   /pilot     pilot portal
 *   /admin     admin portal                 /manager   incident manager portal
 *
 * API (JSON). Everything except /api/login needs a logged-in user:
 *   POST /api/login  /api/logout      GET /api/data
 *   POST /api/report                  pilot            report an incident
 *   POST /api/evidence                pilot / manager  add evidence
 *   POST /api/escalate  /api/delete   admin
 *   POST /api/assign    /api/close    incident manager
 * The service decides whether the logged-in role may do the action.
 */
public class WebServer {

    private static final String COOKIE = "ASIMS_SESSION";

    /** Which observer inboxes each role may read on its dashboard. */
    private static final Map<Role, Set<String>> VISIBLE_NOTIFICATIONS = Map.of(
            Role.PILOT, Set.of(),
            Role.ADMIN, Set.of("Admin Inbox", "Aviation Authority", "Maintenance Team"),
            Role.INCIDENT_MANAGER, Set.of("Incident Management"));

    private final IncidentService service;
    private final AuthService auth;
    private final Gson gson = new Gson();

    public WebServer(IncidentService service, AuthService auth) {
        this.service = service;
        this.auth = auth;
    }

    public void start(int port) throws IOException {
        // Only listen on this computer (localhost), not on the whole network.
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/", this::handle);
        server.start();
        System.out.println("ASIMS is running at http://localhost:" + port);
    }

    // =====================================================================
    // Request handling
    // =====================================================================

    private void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (SecurityException e) {
            sendJson(exchange, 403, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException | JsonParseException e) {
            // e.g. invalid severity, escalating twice -> shown to the user
            sendJson(exchange, 400, Map.of("error", String.valueOf(e.getMessage())));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, Map.of("error", "Something went wrong on the server."));
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Account user = currentUser(exchange);

        switch (path) {
            case "/" -> redirect(exchange, user == null ? "/login" : user.getRole().getHomePath());
            case "/login" -> {
                if (user != null) redirect(exchange, user.getRole().getHomePath());
                else servePage(exchange, "login.html");
            }
            case "/pilot" -> portal(exchange, user, Role.PILOT, "pilot.html");
            case "/admin" -> portal(exchange, user, Role.ADMIN, "admin.html");
            case "/manager" -> portal(exchange, user, Role.INCIDENT_MANAGER, "manager.html");
            case "/static/common.css" -> serveResource(exchange, "common.css", "text/css; charset=utf-8");
            case "/static/common.js" -> serveResource(exchange, "common.js", "application/javascript; charset=utf-8");

            case "/api/login" -> login(exchange);
            case "/api/logout" -> logout(exchange);
            case "/api/data" -> {
                if (user == null) sendJson(exchange, 401, Map.of("error", "Please log in."));
                else sendJson(exchange, 200, buildData(user));
            }

            case "/api/report" -> action(exchange, user, (u, body) ->
                    service.reportIncident(u, body.get("type"), body.get("flightNumber"), body.get("location"),
                            Integer.parseInt(String.valueOf(body.get("severity"))), body.get("aircraft"),
                            new LinkedHashMap<>(body)));
            case "/api/evidence" -> action(exchange, user, (u, body) ->
                    service.addEvidence(u, body.get("incidentId"), body.get("description")));
            case "/api/escalate" -> action(exchange, user, (u, body) ->
                    service.escalateIncident(u, body.get("incidentId")));
            case "/api/assign" -> action(exchange, user, (u, body) ->
                    service.assignInvestigator(u, body.get("incidentId"), body.get("investigatorId")));
            case "/api/close" -> action(exchange, user, (u, body) ->
                    service.closeIncident(u, body.get("incidentId")));
            case "/api/delete" -> action(exchange, user, (u, body) ->
                    service.deleteIncident(u, body.get("incidentId")));

            default -> sendJson(exchange, 404, Map.of("error", "Not found."));
        }
    }

    /** A small interface so each endpoint can pass its action as a lambda. */
    private interface Action {
        void run(Account user, Map<String, String> body);
    }

    private void action(HttpExchange exchange, Account user, Action action) throws IOException {
        if (user == null) {
            sendJson(exchange, 401, Map.of("error", "Please log in."));
            return;
        }
        if (!exchange.getRequestMethod().equals("POST")) {
            sendJson(exchange, 405, Map.of("error", "Use POST."));
            return;
        }
        action.run(user, readBody(exchange));
        sendJson(exchange, 200, Map.of("ok", true));
    }

    // ---------- Login / logout ----------

    private void login(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendJson(exchange, 405, Map.of("error", "Use POST."));
            return;
        }
        Map<String, String> body = readBody(exchange);
        String token = auth.login(body.get("username"), body.get("password"));   // throws if wrong
        Account account = auth.getAccount(token);
        // HttpOnly: JavaScript cannot read the cookie. SameSite=Strict: other sites cannot use it.
        exchange.getResponseHeaders().add("Set-Cookie", COOKIE + "=" + token + "; Path=/; HttpOnly; SameSite=Strict");
        sendJson(exchange, 200, Map.of("redirect", account.getRole().getHomePath()));
    }

    private void logout(HttpExchange exchange) throws IOException {
        auth.logout(readCookie(exchange));
        exchange.getResponseHeaders().add("Set-Cookie", COOKIE + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict");
        sendJson(exchange, 200, Map.of("ok", true));
    }

    private Account currentUser(HttpExchange exchange) {
        return auth.getAccount(readCookie(exchange));
    }

    private String readCookie(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Cookie");
        if (header == null) return null;
        for (String part : header.split(";")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && pair[0].equals(COOKIE)) return pair[1];
        }
        return null;
    }

    // ---------- Pages and files ----------

    private void portal(HttpExchange exchange, Account user, Role required, String file) throws IOException {
        if (user == null) {
            redirect(exchange, "/login");
        } else if (user.getRole() != required) {
            redirect(exchange, user.getRole().getHomePath());   // wrong portal: send them to their own
        } else {
            servePage(exchange, file);
        }
    }

    private void servePage(HttpExchange exchange, String file) throws IOException {
        serveResource(exchange, file, "text/html; charset=utf-8");
    }

    private void serveResource(HttpExchange exchange, String file, String contentType) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/web/" + file)) {
            if (in == null) {
                sendJson(exchange, 404, Map.of("error", "Not found."));
                return;
            }
            byte[] bytes = in.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
    }

    private Map<String, String> readBody(HttpExchange exchange) throws IOException {
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            Map<String, String> body = gson.fromJson(json, new TypeToken<Map<String, String>>() { }.getType());
            return body == null ? new LinkedHashMap<>() : body;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("The request could not be understood.");
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

    // =====================================================================
    // Turning objects into simple maps for JSON (only what this role may see)
    // =====================================================================

    private Map<String, Object> buildData(Account user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("me", Map.of("username", user.getUsername(), "fullName", user.getFullName(),
                "role", user.getRole().getLabel()));

        List<Map<String, Object>> incidents = new ArrayList<>();
        for (Incident incident : service.getIncidentsFor(user)) {
            incidents.add(incidentToMap(incident));
        }
        data.put("incidents", incidents);

        if (user.getRole() == Role.PILOT) {
            List<Map<String, Object>> aircraft = new ArrayList<>();
            for (Aircraft a : service.getFleet().getAircraftList()) {
                aircraft.add(Map.of("registration", a.getRegistration(), "model", a.getModel()));
            }
            data.put("aircraft", aircraft);
        }
        if (user.getRole() == Role.INCIDENT_MANAGER) {
            List<Map<String, Object>> investigators = new ArrayList<>();
            for (Investigator inv : service.getInvestigators()) {
                investigators.add(Map.of("id", inv.getId(), "name", inv.getName(),
                        "specialization", inv.getSpecialization()));
            }
            data.put("investigators", investigators);
        }

        List<Map<String, Object>> observers = new ArrayList<>();
        for (IncidentObserver observer : service.getObservers()) {
            if (VISIBLE_NOTIFICATIONS.get(user.getRole()).contains(observer.getName())) {
                observers.add(Map.of("name", observer.getName(), "notifications", observer.getNotifications()));
            }
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
        map.put("investigator", inv == null ? "" : inv.getName());

        List<Map<String, Object>> evidence = new ArrayList<>();
        for (EvidenceLog log : incident.getEvidenceLogs()) {
            evidence.add(Map.of("id", log.getId(), "description", log.getDescription(),
                    "collectedBy", log.getCollectedBy(), "collectedAt", log.getCollectedAt()));
        }
        map.put("evidence", evidence);
        return map;
    }
}
