package com.asims.repository;

import com.asims.model.Investigator;
import com.asims.model.Pilot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Saves and loads the staff: pilots and investigators. */
public class StaffRepository {

    private final Connection connection;

    public StaffRepository(Database database) {
        this.connection = database.getConnection();
    }

    public void savePilot(Pilot pilot) {
        run("INSERT OR REPLACE INTO pilots(id, name, rank) VALUES(?, ?, ?)",
                pilot.getId(), pilot.getName(), pilot.getRank());
    }

    public void saveInvestigator(Investigator investigator) {
        run("INSERT OR REPLACE INTO investigators(id, name, specialization) VALUES(?, ?, ?)",
                investigator.getId(), investigator.getName(), investigator.getSpecialization());
    }

    public List<Pilot> loadPilots() {
        List<Pilot> list = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name, rank FROM pilots ORDER BY id")) {
            while (rs.next()) {
                list.add(new Pilot(rs.getString("id"), rs.getString("name"), rs.getString("rank")));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load pilots", e);
        }
        return list;
    }

    public List<Investigator> loadInvestigators() {
        List<Investigator> list = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name, specialization FROM investigators ORDER BY id")) {
            while (rs.next()) {
                list.add(new Investigator(rs.getString("id"), rs.getString("name"),
                        rs.getString("specialization")));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load investigators", e);
        }
        return list;
    }

    private void run(String sql, String... values) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) ps.setString(i + 1, values[i]);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not save staff member", e);
        }
    }
}
