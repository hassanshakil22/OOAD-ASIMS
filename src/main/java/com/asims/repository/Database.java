package com.asims.repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The SQLite database: one connection and the table definitions.
 *
 * SQLite stores everything in ONE file (data/asims.db), so there is no database server
 * to install. Tests use an in-memory database that disappears when the test ends.
 *
 * Tables:
 *   aircraft, pilots, investigators   the fleet and the staff
 *   accounts                          login accounts (password stored as a salted hash)
 *   incidents                         one row per incident; the type-specific fields are JSON in "extra"
 *   evidence                          evidence logs; deleted automatically with their incident
 *                                     (ON DELETE CASCADE) — composition, in SQL form
 */
public class Database {

    private final Connection connection;

    private Database(String url) {
        try {
            connection = DriverManager.getConnection(url);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");   // SQLite ignores foreign keys unless asked
            }
            createTables();
        } catch (SQLException e) {
            throw new DatabaseException("Could not open the database", e);
        }
    }

    /** A database stored in a file (created if it does not exist yet). */
    public static Database openFile(Path file) {
        try {
            Path folder = file.toAbsolutePath().getParent();
            if (folder != null) Files.createDirectories(folder);
        } catch (java.io.IOException e) {
            throw new DatabaseException("Could not create the data folder", e);
        }
        return new Database("jdbc:sqlite:" + file.toAbsolutePath());
    }

    /** A temporary database that lives only in memory (used by the tests). */
    public static Database inMemory() {
        return new Database("jdbc:sqlite::memory:");
    }

    public Connection getConnection() {
        return connection;
    }

    private void createTables() throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS aircraft ("
                    + "registration TEXT PRIMARY KEY, model TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS pilots ("
                    + "id TEXT PRIMARY KEY, name TEXT NOT NULL, rank TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS investigators ("
                    + "id TEXT PRIMARY KEY, name TEXT NOT NULL, specialization TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS accounts ("
                    + "username TEXT PRIMARY KEY, full_name TEXT NOT NULL, role TEXT NOT NULL, "
                    + "staff_id TEXT, password_hash TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS incidents ("
                    + "id TEXT PRIMARY KEY, type TEXT NOT NULL, flight_number TEXT NOT NULL, "
                    + "location TEXT NOT NULL, severity INTEGER NOT NULL, status TEXT NOT NULL, "
                    + "reported_at TEXT NOT NULL, extra TEXT NOT NULL, "
                    + "aircraft_reg TEXT NOT NULL REFERENCES aircraft(registration), "
                    + "pilot_id TEXT NOT NULL REFERENCES pilots(id), "
                    + "investigator_id TEXT REFERENCES investigators(id))");
            s.execute("CREATE TABLE IF NOT EXISTS evidence ("
                    + "id TEXT PRIMARY KEY, description TEXT NOT NULL, collected_by TEXT NOT NULL, "
                    + "collected_at TEXT NOT NULL, "
                    + "incident_id TEXT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE)");
        }
    }
}
