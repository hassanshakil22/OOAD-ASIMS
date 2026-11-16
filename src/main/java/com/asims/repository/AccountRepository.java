package com.asims.repository;

import com.asims.auth.Account;
import com.asims.auth.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Saves and loads login accounts. Only password HASHES are stored, never passwords. */
public class AccountRepository {

    private final Connection connection;

    public AccountRepository(Database database) {
        this.connection = database.getConnection();
    }

    public void save(Account account) {
        String sql = "INSERT OR REPLACE INTO accounts(username, full_name, role, staff_id, password_hash) "
                + "VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, account.getUsername());
            ps.setString(2, account.getFullName());
            ps.setString(3, account.getRole().name());
            ps.setString(4, account.getStaffId());
            ps.setString(5, account.getPasswordHash());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not save account " + account.getUsername(), e);
        }
    }

    /** Returns the account, or null if the username does not exist. */
    public Account findByUsername(String username) {
        String sql = "SELECT username, full_name, role, staff_id, password_hash FROM accounts WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Account(rs.getString("username"), rs.getString("full_name"),
                        Role.valueOf(rs.getString("role")), rs.getString("staff_id"),
                        rs.getString("password_hash"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not look up account " + username, e);
        }
    }

    public int count() {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM accounts")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Could not count accounts", e);
        }
    }
}
