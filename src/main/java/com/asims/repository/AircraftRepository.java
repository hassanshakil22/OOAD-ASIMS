package com.asims.repository;

import com.asims.model.Aircraft;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Saves and loads Aircraft (the rows that the Fleet aggregates). */
public class AircraftRepository {

    private final Connection connection;

    public AircraftRepository(Database database) {
        this.connection = database.getConnection();
    }

    public void save(Aircraft aircraft) {
        String sql = "INSERT OR REPLACE INTO aircraft(registration, model) VALUES(?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, aircraft.getRegistration());
            ps.setString(2, aircraft.getModel());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not save aircraft " + aircraft.getRegistration(), e);
        }
    }

    public List<Aircraft> loadAll() {
        List<Aircraft> list = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT registration, model FROM aircraft ORDER BY registration")) {
            while (rs.next()) {
                list.add(new Aircraft(rs.getString("registration"), rs.getString("model")));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load aircraft", e);
        }
        return list;
    }
}
