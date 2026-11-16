package com.asims;

import com.asims.auth.Account;
import com.asims.auth.AuthService;
import com.asims.auth.PasswordHasher;
import com.asims.auth.Role;
import com.asims.model.Aircraft;
import com.asims.model.AirspaceIncident;
import com.asims.model.Fleet;
import com.asims.model.Incident;
import com.asims.model.IncidentStatus;
import com.asims.model.Investigator;
import com.asims.model.Pilot;
import com.asims.model.StaffMember;
import com.asims.model.TechnicalIncident;
import com.asims.model.WeatherIncident;
import com.asims.pattern.AdminNotifier;
import com.asims.pattern.AuthorityNotifier;
import com.asims.pattern.IncidentFactory;
import com.asims.pattern.IncidentManagementNotifier;
import com.asims.pattern.IncidentRegistry;
import com.asims.pattern.IncidentService;
import com.asims.pattern.MaintenanceNotifier;
import com.asims.repository.AccountRepository;
import com.asims.repository.AircraftRepository;
import com.asims.repository.Database;
import com.asims.repository.IncidentRepository;
import com.asims.repository.SeedData;
import com.asims.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated tests — one test per OOP concept, design pattern, role rule and database feature.
 * They use a temporary in-memory SQLite database, so nothing is written to disk.
 * Run with:  mvnw.cmd test
 */
class AsimsTest {

    private Database database;
    private IncidentRepository incidentRepo;
    private Fleet fleet;
    private Aircraft boeing;
    private Pilot pilot;
    private Pilot otherPilot;
    private Investigator sara;
    private IncidentService service;
    private AuthorityNotifier authority;
    private MaintenanceNotifier maintenance;
    private AdminNotifier adminInbox;
    private IncidentManagementNotifier management;

    private Account pilotAccount;
    private Account otherPilotAccount;
    private Account admin;
    private Account manager;

    @BeforeEach
    void setUp() {
        IncidentRegistry.getInstance().clear();   // fresh registry for each test

        database = Database.inMemory();
        boeing = new Aircraft("AP-BHV", "Boeing 777-300ER");
        new AircraftRepository(database).save(boeing);
        pilot = new Pilot("P1", "Capt. Tariq Mahmood", "Captain");
        otherPilot = new Pilot("P2", "F/O Hina Raza", "First Officer");
        sara = new Investigator("I1", "Sara Ahmed", "Aircraft Systems");
        StaffRepository staffRepo = new StaffRepository(database);
        staffRepo.savePilot(pilot);
        staffRepo.savePilot(otherPilot);
        staffRepo.saveInvestigator(sara);

        fleet = new Fleet("Test Airline");
        fleet.addAircraft(boeing);
        incidentRepo = new IncidentRepository(database);

        service = new IncidentService(IncidentRegistry.getInstance(), fleet, incidentRepo);
        service.addPilot(pilot);
        service.addPilot(otherPilot);
        service.addInvestigator(sara);
        authority = new AuthorityNotifier();
        maintenance = new MaintenanceNotifier();
        adminInbox = new AdminNotifier();
        management = new IncidentManagementNotifier();
        service.addObserver(authority);
        service.addObserver(maintenance);
        service.addObserver(adminInbox);
        service.addObserver(management);

        String hash = PasswordHasher.hash("secret");
        pilotAccount = new Account("tariq", pilot.getName(), Role.PILOT, "P1", hash);
        otherPilotAccount = new Account("hina", otherPilot.getName(), Role.PILOT, "P2", hash);
        admin = new Account("admin", "Bilal Qureshi", Role.ADMIN, null, hash);
        manager = new Account("manager", "Nadia Hussain", Role.INCIDENT_MANAGER, null, hash);
    }

    private Incident report(boolean grounded) {
        return service.reportIncident(pilotAccount, "technical", "PK-304", "OPKC", 4, "AP-BHV",
                Map.of("aircraftSystem", "Hydraulics", "grounded", String.valueOf(grounded)));
    }

    // =====================================================================
    // OOP concepts and design patterns
    // =====================================================================

    @Test
    @DisplayName("T1 Singleton: getInstance() always returns the same object")
    void singletonReturnsSameInstance() {
        assertSame(IncidentRegistry.getInstance(), IncidentRegistry.getInstance());
    }

    @Test
    @DisplayName("T2 Factory: creates the correct subclass for each type")
    void factoryCreatesCorrectSubclass() {
        Map<String, String> none = Map.of();
        assertInstanceOf(TechnicalIncident.class, IncidentFactory.create("technical", "X1", "PK-1", "A", 3, boeing, pilot, none));
        assertInstanceOf(WeatherIncident.class, IncidentFactory.create("weather", "X2", "PK-2", "A", 3, boeing, pilot, none));
        assertInstanceOf(AirspaceIncident.class, IncidentFactory.create("airspace", "X3", "PK-3", "A", 3, boeing, pilot, none));
        assertThrows(IllegalArgumentException.class,
                () -> IncidentFactory.create("volcano", "X4", "PK-4", "A", 3, boeing, pilot, none));
    }

    @Test
    @DisplayName("T3 Polymorphism: same severity, different risk score per type")
    void polymorphicRiskScores() {
        Incident tech = new TechnicalIncident("A", "PK-1", "X", 3, boeing, pilot, "Engine", true);
        Incident weather = new WeatherIncident("B", "PK-2", "X", 3, boeing, pilot, "Turbulence", 2);
        Incident airspace = new AirspaceIncident("C", "PK-3", "X", 3, boeing, pilot, 0.5);

        assertEquals(76, tech.calculateRiskScore());     // 3*12 + 25 grounded + 15 critical system
        assertEquals(50, weather.calculateRiskScore());  // 3*10 + 2*10
        assertEquals(80, airspace.calculateRiskScore()); // 3*10 + 50 (separation < 1 nm)
    }

    @Test
    @DisplayName("T4 Inheritance / Liskov: every subclass works as an Incident or StaffMember")
    void subclassesAreSubstitutable() {
        List<Incident> incidents = List.of(
                new TechnicalIncident("A", "PK-1", "X", 2, boeing, pilot, "Avionics", false),
                new WeatherIncident("B", "PK-2", "X", 2, boeing, pilot, "Icing", 1),
                new AirspaceIncident("C", "PK-3", "X", 2, boeing, pilot, 4.0));
        for (Incident incident : incidents) {
            int score = incident.calculateRiskScore();
            assertTrue(score >= 0 && score <= 100);
            assertEquals(IncidentStatus.REPORTED, incident.getStatus());
            assertFalse(incident.getExtraFields().isEmpty());
        }

        List<StaffMember> staff = List.of(pilot, sara);
        assertEquals("Pilot (Captain)", staff.get(0).getRole());
        assertEquals("Safety Investigator", staff.get(1).getRole());
    }

    @Test
    @DisplayName("T5 Encapsulation: invalid severity is rejected")
    void invalidSeverityRejected() {
        Incident incident = report(true);
        assertThrows(IllegalArgumentException.class, () -> incident.setSeverity(9));
        assertThrows(IllegalArgumentException.class, () -> incident.setSeverity(0));
        assertEquals(4, incident.getSeverity());   // unchanged
    }

    @Test
    @DisplayName("T6 Encapsulation: evidence list cannot be modified from outside")
    void evidenceListIsReadOnly() {
        Incident incident = report(true);
        assertThrows(UnsupportedOperationException.class, () -> incident.getEvidenceLogs().clear());
    }

    @Test
    @DisplayName("T7 Observer: authority is notified on report and escalation")
    void authorityNotified() {
        Incident incident = report(false);
        service.escalateIncident(admin, incident.getId());

        assertEquals(2, authority.getNotifications().size());
        assertTrue(authority.getNotifications().get(0).startsWith("REPORTED"));
        assertTrue(authority.getNotifications().get(1).startsWith("ESCALATED"));
    }

    @Test
    @DisplayName("T8 Observer: maintenance is only notified for grounded aircraft")
    void maintenanceOnlyForGrounded() {
        report(false);
        assertEquals(0, maintenance.getNotifications().size());
        report(true);
        assertEquals(1, maintenance.getNotifications().size());
    }

    @Test
    @DisplayName("T9 Observer: admin inbox gets new reports, incident management gets escalations")
    void roleInboxesNotified() {
        Incident incident = report(true);
        assertEquals(1, adminInbox.getNotifications().size());
        assertEquals(0, management.getNotifications().size());   // not escalated yet

        service.escalateIncident(admin, incident.getId());
        assertEquals(1, adminInbox.getNotifications().size());   // unchanged
        assertEquals(1, management.getNotifications().size());
    }

    @Test
    @DisplayName("T10 Composition: evidence is deleted with its incident, in memory and in the database")
    void compositionEvidenceDeletedWithIncident() {
        Incident incident = report(true);
        service.addEvidence(pilotAccount, incident.getId(), "Fluid leak photo");
        assertEquals(1, incident.getEvidenceLogs().size());
        assertEquals(1, incidentRepo.countEvidence(incident.getId()));

        service.deleteIncident(admin, incident.getId());
        assertTrue(service.getIncidentsFor(admin).isEmpty());
        assertEquals(0, incidentRepo.countEvidence(incident.getId()));   // removed by ON DELETE CASCADE
    }

    @Test
    @DisplayName("T11 Aggregation: aircraft still exists after leaving the fleet")
    void aggregationAircraftSurvives() {
        Incident incident = report(true);
        fleet.removeAircraft(boeing);

        assertTrue(fleet.getAircraftList().isEmpty());
        assertEquals("AP-BHV", incident.getAircraft().getRegistration());   // still alive
    }

    // =====================================================================
    // Roles and workflow
    // =====================================================================

    @Test
    @DisplayName("T12 Workflow: pilot reports -> admin escalates -> manager assigns -> manager closes")
    void fullWorkflowAcrossRoles() {
        Incident incident = report(true);
        assertEquals(IncidentStatus.REPORTED, incident.getStatus());

        // steps cannot be skipped
        assertThrows(IllegalStateException.class, () -> service.assignInvestigator(manager, incident.getId(), "I1"));
        assertThrows(IllegalStateException.class, () -> service.closeIncident(manager, incident.getId()));

        service.escalateIncident(admin, incident.getId());
        assertEquals(IncidentStatus.ESCALATED, incident.getStatus());
        assertThrows(IllegalStateException.class, () -> service.escalateIncident(admin, incident.getId()));  // not twice
        assertThrows(IllegalStateException.class, () -> service.closeIncident(manager, incident.getId()));   // needs an investigator

        service.assignInvestigator(manager, incident.getId(), "I1");
        assertEquals(IncidentStatus.UNDER_INVESTIGATION, incident.getStatus());
        assertEquals("Sara Ahmed", incident.getAssignedInvestigator().getName());

        service.closeIncident(manager, incident.getId());
        assertEquals(IncidentStatus.CLOSED, incident.getStatus());
        assertThrows(IllegalStateException.class, () -> service.escalateIncident(admin, incident.getId()));
    }

    @Test
    @DisplayName("T13 Roles: each role can only do its own actions")
    void rolePermissions() {
        Incident incident = report(true);
        String id = incident.getId();

        assertThrows(SecurityException.class, () -> service.escalateIncident(pilotAccount, id));
        assertThrows(SecurityException.class, () -> service.deleteIncident(pilotAccount, id));
        assertThrows(SecurityException.class, () -> service.assignInvestigator(admin, id, "I1"));
        assertThrows(SecurityException.class, () -> service.closeIncident(admin, id));
        assertThrows(SecurityException.class, () ->
                service.reportIncident(admin, "weather", "PK-1", "X", 3, "AP-BHV", Map.of()));
        assertThrows(SecurityException.class, () ->
                service.reportIncident(manager, "weather", "PK-1", "X", 3, "AP-BHV", Map.of()));
        assertEquals(IncidentStatus.REPORTED, incident.getStatus());   // nothing changed
    }

    @Test
    @DisplayName("T14 Visibility: pilots see only their own reports; managers only escalated ones")
    void incidentVisibility() {
        Incident mine = report(true);
        service.reportIncident(otherPilotAccount, "weather", "PK-731", "OPLA", 3, "AP-BHV",
                Map.of("weatherType", "Icing", "turbulenceLevel", "2"));

        assertEquals(1, service.getIncidentsFor(pilotAccount).size());
        assertEquals(mine.getId(), service.getIncidentsFor(pilotAccount).get(0).getId());
        assertEquals(2, service.getIncidentsFor(admin).size());
        assertEquals(0, service.getIncidentsFor(manager).size());     // nothing escalated yet

        service.escalateIncident(admin, mine.getId());
        assertEquals(1, service.getIncidentsFor(manager).size());

        // a pilot cannot attach evidence to another pilot's incident
        assertThrows(SecurityException.class,
                () -> service.addEvidence(otherPilotAccount, mine.getId(), "not my incident"));
    }

    // =====================================================================
    // Login and database
    // =====================================================================

    @Test
    @DisplayName("T15 Login: correct password works, wrong password is rejected, passwords are stored hashed")
    void loginAndPasswordHashing() {
        AccountRepository accounts = new AccountRepository(database);
        accounts.save(admin);
        AuthService auth = new AuthService(accounts);

        String token = auth.login("admin", "secret");
        assertEquals("Bilal Qureshi", auth.getAccount(token).getFullName());
        assertThrows(IllegalArgumentException.class, () -> auth.login("admin", "wrong"));
        assertThrows(IllegalArgumentException.class, () -> auth.login("nobody", "secret"));

        auth.logout(token);
        assertNull(auth.getAccount(token));

        String stored = accounts.findByUsername("admin").getPasswordHash();
        assertFalse(stored.contains("secret"));                              // never the real password
        assertNotEquals(PasswordHasher.hash("secret"), PasswordHasher.hash("secret"));   // random salt
    }

    @Test
    @DisplayName("T16 Database: incidents survive a restart as the correct subclass with status and evidence")
    void persistenceRoundTrip() {
        Incident incident = report(true);
        service.addEvidence(pilotAccount, incident.getId(), "Hydraulic pressure 0 psi");
        service.escalateIncident(admin, incident.getId());
        service.assignInvestigator(manager, incident.getId(), "I1");
        String id = incident.getId();

        // "restart": a fresh registry and service that load everything from the same database
        IncidentRegistry.getInstance().clear();
        IncidentService restarted = new IncidentService(IncidentRegistry.getInstance(), fleet, incidentRepo);
        restarted.addPilot(pilot);
        restarted.addInvestigator(sara);
        restarted.loadFromDatabase();

        Incident loaded = restarted.getIncidentsFor(admin).get(0);
        assertEquals(id, loaded.getId());
        assertInstanceOf(TechnicalIncident.class, loaded);
        assertEquals(IncidentStatus.UNDER_INVESTIGATION, loaded.getStatus());
        assertEquals("Sara Ahmed", loaded.getAssignedInvestigator().getName());
        assertEquals(1, loaded.getEvidenceLogs().size());
        assertEquals(incident.calculateRiskScore(), loaded.calculateRiskScore());
        assertEquals(incident.getReportedAt(), loaded.getReportedAt());

        // new ids continue after the loaded ones instead of clashing
        Incident next = restarted.reportIncident(pilotAccount, "weather", "PK-9", "OPLA", 2, "AP-BHV",
                Map.of("weatherType", "Icing", "turbulenceLevel", "1"));
        assertNotEquals(id, next.getId());
    }

    @Test
    @DisplayName("T17 Seed data: the first run creates realistic data and working demo logins")
    void seedData() {
        Database fresh = Database.inMemory();
        AircraftRepository aircraftRepo = new AircraftRepository(fresh);
        StaffRepository staffRepo = new StaffRepository(fresh);
        AccountRepository accountRepo = new AccountRepository(fresh);
        IncidentRepository incidents = new IncidentRepository(fresh);
        SeedData.seedIfEmpty(aircraftRepo, staffRepo, accountRepo, incidents);

        assertEquals(8, aircraftRepo.loadAll().size());
        assertEquals(4, staffRepo.loadPilots().size());
        assertEquals(6, accountRepo.count());

        AuthService auth = new AuthService(accountRepo);
        assertEquals(Role.ADMIN, auth.getAccount(auth.login("admin", "admin123")).getRole());
        assertEquals(Role.INCIDENT_MANAGER, auth.getAccount(auth.login("manager", "manager123")).getRole());
        assertEquals(Role.PILOT, auth.getAccount(auth.login("tariq", "pilot123")).getRole());

        // running it again must not add anything
        SeedData.seedIfEmpty(aircraftRepo, staffRepo, accountRepo, incidents);
        assertEquals(6, accountRepo.count());
    }
}
