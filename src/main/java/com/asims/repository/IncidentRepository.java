package com.asims.repository;

import com.asims.model.Fleet;
import com.asims.model.Incident;
import com.asims.model.IncidentStatus;
import com.asims.model.Investigator;
import com.asims.model.Pilot;
import com.asims.pattern.IncidentFactory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Saves incidents (with their evidence) to the database and loads them back.
 *
 * Loading goes through IncidentFactory, so every row comes back as the right subclass
 * (TechnicalIncident, WeatherIncident or AirspaceIncident) — the Factory pattern again.
 * The type-specific fields are kept as JSON text, taken from Incident.getExtraFields()
 * (polymorphism): this class never needs to know which kind of incident it is saving.
 */
public class IncidentRepository {

    private final Connection connection;
    private final Gson gson = new Gson();

    public IncidentRepository(Database database) {
        this.connection = database.getConnection();
    }

    /** Inserts the incident, or updates it if it already exists. Also saves its evidence. */
    public void save(Incident incident) {
        // Not "INSERT OR REPLACE": that would delete the row first, and with it all its evidence.
        String sql = "INSERT INTO incidents(id, type, flight_number, location, severity, status, reported_at, "
                + "extra, aircraft_reg, pilot_id, investigator_id) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(id) DO UPDATE SET severity = excluded.severity, status = excluded.status, "
                + "investigator_id = excluded.investigator_id";
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, incident.getId());
                ps.setString(2, incident.getType().toLowerCase());
                ps.setString(3, incident.getFlightNumber());
                ps.setString(4, incident.getLocation());
                ps.setInt(5, incident.getSeverity());
                ps.setString(6, incident.getStatus().name());
                ps.setString(7, incident.getReportedAt());
                ps.setString(8, gson.toJson(incident.getExtraFields()));
                ps.setString(9, incident.getAircraft().getRegistration());
                ps.setString(10, incident.getReportedBy().getId());
                Investigator investigator = incident.getAssignedInvestigator();
                ps.setString(11, investigator == null ? null : investigator.getId());
                ps.executeUpdate();
            }
            String evidenceSql = "INSERT OR IGNORE INTO evidence(id, description, collected_by, collected_at, incident_id) "
                    + "VALUES(?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(evidenceSql)) {
                for (var log : incident.getEvidenceLogs()) {
                    ps.setString(1, log.getId());
                    ps.setString(2, log.getDescription());
                    ps.setString(3, log.getCollectedBy());
                    ps.setString(4, log.getCollectedAt());
                    ps.setString(5, incident.getId());
                    ps.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new DatabaseException("Could not save incident " + incident.getId(), e);
        } finally {
            restoreAutoCommit();
        }
    }

    /** Deletes the incident. The database deletes its evidence automatically (ON DELETE CASCADE). */
    public void delete(String incidentId) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM incidents WHERE id = ?")) {
            ps.setString(1, incidentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete incident " + incidentId, e);
        }
    }

    /** Loads every incident, rebuilt as the correct subclass with its evidence. */
    public List<Incident> loadAll(Fleet fleet, Map<String, Pilot> pilots, Map<String, Investigator> investigators) {
        List<Incident> incidents = new ArrayList<>();
        String sql = "SELECT * FROM incidents ORDER BY CAST(SUBSTR(id, 5) AS INTEGER)";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, String> extra = gson.fromJson(rs.getString("extra"),
                        new TypeToken<Map<String, String>>() { }.getType());
                Incident incident = IncidentFactory.create(rs.getString("type"), rs.getString("id"),
                        rs.getString("flight_number"), rs.getString("location"), rs.getInt("severity"),
                        fleet.findAircraft(rs.getString("aircraft_reg")),
                        pilots.get(rs.getString("pilot_id")), extra);
                String investigatorId = rs.getString("investigator_id");
                incident.restoreState(rs.getString("reported_at"),
                        IncidentStatus.valueOf(rs.getString("status")),
                        investigatorId == null ? null : investigators.get(investigatorId));
                incidents.add(incident);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load incidents", e);
        }
        for (Incident incident : incidents) {
            loadEvidence(incident);
        }
        return incidents;
    }

    private void loadEvidence(Incident incident) {
        String sql = "SELECT id, description, collected_by, collected_at FROM evidence WHERE incident_id = ? ORDER BY rowid";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, incident.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    incident.restoreEvidence(rs.getString("id"), rs.getString("description"),
                            rs.getString("collected_by"), rs.getString("collected_at"));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load evidence for " + incident.getId(), e);
        }
    }

    public int countEvidence(String incidentId) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM evidence WHERE incident_id = ?")) {
            ps.setString(1, incidentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not count evidence", e);
        }
    }

    private void rollbackQuietly() {
        try { connection.rollback(); } catch (SQLException ignored) { }
    }

    private void restoreAutoCommit() {
        try { connection.setAutoCommit(true); } catch (SQLException ignored) { }
    }
}
