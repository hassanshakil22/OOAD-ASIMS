package com.asims;

import com.asims.model.AirspaceIncident;
import com.asims.model.Aircraft;
import com.asims.model.Fleet;
import com.asims.model.Incident;
import com.asims.model.IncidentStatus;
import com.asims.model.Investigator;
import com.asims.model.Pilot;
import com.asims.model.StaffMember;
import com.asims.model.TechnicalIncident;
import com.asims.model.WeatherIncident;
import com.asims.pattern.AuthorityNotifier;
import com.asims.pattern.IncidentFactory;
import com.asims.pattern.IncidentRegistry;
import com.asims.pattern.IncidentService;
import com.asims.pattern.MaintenanceNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated tests — one test per OOP concept / design pattern.
 * Run with:  mvnw.cmd test
 */
class AsimsTest {

    private Fleet fleet;
    private Aircraft boeing;
    private Pilot pilot;
    private IncidentService service;
    private AuthorityNotifier authority;
    private MaintenanceNotifier maintenance;

    @BeforeEach
    void setUp() {
        IncidentRegistry.getInstance().clear();   // fresh registry for each test

        boeing = new Aircraft("AP-BXK", "Boeing 777-300ER");
        fleet = new Fleet("Test Airline");
        fleet.addAircraft(boeing);
        pilot = new Pilot("P1", "Capt. Tariq Mahmood", "Captain");

        service = new IncidentService(IncidentRegistry.getInstance(), fleet);
        service.addPilot(pilot);
        service.addInvestigator(new Investigator("I1", "Sara Ahmed", "Aircraft Systems"));
        authority = new AuthorityNotifier();
        maintenance = new MaintenanceNotifier();
        service.addObserver(authority);
        service.addObserver(maintenance);
    }

    private Incident technical(boolean grounded) {
        return service.reportIncident("technical", "PK-304", "OPKC", 4, "AP-BXK", "P1",
                Map.of("aircraftSystem", "Hydraulics", "grounded", String.valueOf(grounded)));
    }

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
        }

        List<StaffMember> staff = List.of(pilot, new Investigator("I9", "Omar Farooq", "ATC"));
        assertEquals("Pilot (Captain)", staff.get(0).getRole());
        assertEquals("Safety Investigator", staff.get(1).getRole());
    }

    @Test
    @DisplayName("T5 Encapsulation: invalid severity is rejected")
    void invalidSeverityRejected() {
        Incident incident = technical(true);
        assertThrows(IllegalArgumentException.class, () -> incident.setSeverity(9));
        assertThrows(IllegalArgumentException.class, () -> incident.setSeverity(0));
        assertEquals(4, incident.getSeverity());   // unchanged
    }

    @Test
    @DisplayName("T6 Encapsulation: evidence list cannot be modified from outside")
    void evidenceListIsReadOnly() {
        Incident incident = technical(true);
        assertThrows(UnsupportedOperationException.class, () -> incident.getEvidenceLogs().clear());
    }

    @Test
    @DisplayName("T7 Observer: authority is notified on report and escalation")
    void authorityNotified() {
        Incident incident = technical(false);
        service.escalateIncident(incident.getId());

        assertEquals(2, authority.getNotifications().size());
        assertTrue(authority.getNotifications().get(0).startsWith("REPORTED"));
        assertTrue(authority.getNotifications().get(1).startsWith("ESCALATED"));
    }

    @Test
    @DisplayName("T8 Observer: maintenance is only notified for grounded aircraft")
    void maintenanceOnlyForGrounded() {
        technical(false);
        assertEquals(0, maintenance.getNotifications().size());
        technical(true);
        assertEquals(1, maintenance.getNotifications().size());
    }

    @Test
    @DisplayName("T9 Composition: evidence belongs to its incident and is deleted with it")
    void compositionEvidenceDeletedWithIncident() {
        Incident incident = technical(true);
        service.addEvidence(incident.getId(), "Fluid leak photo", "Sara Ahmed");
        assertEquals(1, incident.getEvidenceLogs().size());

        service.deleteIncident(incident.getId());
        assertTrue(service.getAllIncidents().isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> service.addEvidence(incident.getId(), "more", "Sara Ahmed"));
    }

    @Test
    @DisplayName("T10 Aggregation: aircraft still exists after leaving the fleet")
    void aggregationAircraftSurvives() {
        Incident incident = technical(true);
        fleet.removeAircraft(boeing);

        assertTrue(fleet.getAircraftList().isEmpty());
        assertEquals("AP-BXK", incident.getAircraft().getRegistration());   // still alive
    }

    @Test
    @DisplayName("T11 Workflow: report -> assign -> escalate -> close")
    void fullWorkflow() {
        Incident incident = technical(true);
        assertEquals(IncidentStatus.REPORTED, incident.getStatus());

        assertThrows(IllegalStateException.class, () -> service.closeIncident(incident.getId()));

        service.assignInvestigator(incident.getId(), "I1");
        assertEquals(IncidentStatus.UNDER_INVESTIGATION, incident.getStatus());
        assertEquals("Sara Ahmed", incident.getAssignedInvestigator().getName());

        service.escalateIncident(incident.getId());
        assertEquals(IncidentStatus.ESCALATED, incident.getStatus());

        service.closeIncident(incident.getId());
        assertEquals(IncidentStatus.CLOSED, incident.getStatus());
        assertThrows(IllegalStateException.class, () -> service.escalateIncident(incident.getId()));
    }
}
