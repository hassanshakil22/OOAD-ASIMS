package com.asims.repository;

import com.asims.auth.Account;
import com.asims.auth.PasswordHasher;
import com.asims.auth.Role;
import com.asims.model.Aircraft;
import com.asims.model.Incident;
import com.asims.model.IncidentStatus;
import com.asims.model.Investigator;
import com.asims.model.Pilot;
import com.asims.pattern.IncidentFactory;

import java.util.Map;

/**
 * Fills an empty database with realistic starting data the first time the program runs.
 *
 * The airports (OPKC Karachi, OPLA Lahore, OPIS Islamabad, OPPS Peshawar, OPMT Multan),
 * routes, aircraft types and incident scenarios are real-world style. The people and the
 * exact aircraft registrations are made up, so no real person or airframe is named.
 *
 * Demo logins (password in brackets):
 *   admin (admin123)   manager (manager123)   tariq / hina / imran / zainab (pilot123)
 */
public final class SeedData {

    private SeedData() { }

    public static void seedIfEmpty(AircraftRepository aircraftRepo, StaffRepository staffRepo,
                                   AccountRepository accountRepo, IncidentRepository incidentRepo) {
        if (accountRepo.count() > 0) {
            return;   // already seeded on an earlier run
        }

        // ---------- Fleet (a Pakistani flag-carrier style fleet) ----------
        Aircraft b777a = new Aircraft("AP-BHV", "Boeing 777-300ER");
        Aircraft b777b = new Aircraft("AP-BID", "Boeing 777-300ER");
        Aircraft b777c = new Aircraft("AP-BGJ", "Boeing 777-200ER");
        Aircraft a320a = new Aircraft("AP-BLP", "Airbus A320-200");
        Aircraft a320b = new Aircraft("AP-BLV", "Airbus A320-200");
        Aircraft a320c = new Aircraft("AP-BLC", "Airbus A320-200");
        Aircraft atr1 = new Aircraft("AP-BHO", "ATR 72-500");
        Aircraft atr2 = new Aircraft("AP-BHP", "ATR 72-500");
        for (Aircraft a : new Aircraft[]{b777a, b777b, b777c, a320a, a320b, a320c, atr1, atr2}) {
            aircraftRepo.save(a);
        }

        // ---------- Staff ----------
        Pilot tariq = new Pilot("P1", "Capt. Tariq Mahmood", "Captain");
        Pilot hina = new Pilot("P2", "F/O Hina Raza", "First Officer");
        Pilot imran = new Pilot("P3", "Capt. Imran Siddiqui", "Captain");
        Pilot zainab = new Pilot("P4", "F/O Zainab Malik", "First Officer");
        for (Pilot p : new Pilot[]{tariq, hina, imran, zainab}) staffRepo.savePilot(p);

        Investigator sara = new Investigator("I1", "Sara Ahmed", "Aircraft Systems");
        Investigator omar = new Investigator("I2", "Omar Farooq", "Air Traffic & Human Factors");
        Investigator hassan = new Investigator("I3", "Hassan Shakil", "Head of Civil Aviation Safety");
        Investigator ayan = new Investigator("I4", "Ayan Mirza", "Office boy");
        for (Investigator i : new Investigator[]{sara, omar, hassan, ayan}) staffRepo.saveInvestigator(i);

        // ---------- Login accounts ----------
        accountRepo.save(new Account("admin", "Bilal Qureshi", Role.ADMIN, null, PasswordHasher.hash("admin123")));
        accountRepo.save(new Account("manager", "Nadia Hussain", Role.INCIDENT_MANAGER, null, PasswordHasher.hash("manager123")));
        accountRepo.save(new Account("tariq", tariq.getName(), Role.PILOT, "P1", PasswordHasher.hash("pilot123")));
        accountRepo.save(new Account("hina", hina.getName(), Role.PILOT, "P2", PasswordHasher.hash("pilot123")));
        accountRepo.save(new Account("imran", imran.getName(), Role.PILOT, "P3", PasswordHasher.hash("pilot123")));
        accountRepo.save(new Account("zainab", zainab.getName(), Role.PILOT, "P4", PasswordHasher.hash("pilot123")));

        // ---------- Incidents in every stage, so every portal has something to show ----------
        // Closed cases (history)
        Incident i101 = incident("technical", "INC-101", "PK-309", "Karachi (OPKC)", 5, b777a, tariq,
                Map.of("aircraftSystem", "Engine", "grounded", "true"),
                "2026-10-21 05:55", IncidentStatus.CLOSED, sara);
        i101.restoreEvidence("INC-101-E1", "No. 2 engine oil pressure fell below limits during takeoff roll; takeoff rejected at 90 kt", "Capt. Tariq Mahmood", "2026-10-21 06:10");
        i101.restoreEvidence("INC-101-E2", "Borescope inspection found oil pump drive-shaft wear; engine replaced", "Sara Ahmed", "2026-10-24 11:30");
        Incident i102 = incident("weather", "INC-102", "PK-702", "Multan (OPMT)", 2, atr1, hina,
                Map.of("weatherType", "Icing", "turbulenceLevel", "2"),
                "2026-10-28 07:40", IncidentStatus.CLOSED, omar);
        i102.restoreEvidence("INC-102-E1", "Light airframe icing in cloud at FL150; anti-ice used, no performance loss", "F/O Hina Raza", "2026-10-28 08:05");

        // Under investigation (incident manager is working on them)
        Incident i103 = incident("technical", "INC-103", "PK-304", "Karachi (OPKC)", 4, b777b, tariq,
                Map.of("aircraftSystem", "Hydraulics", "grounded", "true"),
                "2026-11-03 06:42", IncidentStatus.UNDER_INVESTIGATION, sara);
        i103.restoreEvidence("INC-103-E1", "Hydraulic system B pressure dropped to 0 psi during climb", "Capt. Tariq Mahmood", "2026-11-03 07:05");
        i103.restoreEvidence("INC-103-E2", "Fluid leak found near left main landing gear actuator", "Sara Ahmed", "2026-11-04 09:20");
        Incident i104 = incident("airspace", "INC-104", "PK-370", "Islamabad approach (OPIS)", 4, a320a, imran,
                Map.of("separationNm", "1.6"),
                "2026-11-07 17:12", IncidentStatus.UNDER_INVESTIGATION, omar);
        i104.restoreEvidence("INC-104-E1", "TCAS traffic advisory; separation reduced to 1.6 nm against a departing ATR", "Capt. Imran Siddiqui", "2026-11-07 17:40");

        // Escalated by the admin, waiting for the incident manager
        Incident i105 = incident("weather", "INC-105", "PK-731", "Near Lahore (OPLA)", 3, atr2, hina,
                Map.of("weatherType", "Severe Turbulence", "turbulenceLevel", "4"),
                "2026-11-10 14:20", IncidentStatus.ESCALATED, null);
        i105.restoreEvidence("INC-105-E1", "Two cabin crew injured; seatbelt sign was on, convective cell not visible on radar", "F/O Hina Raza", "2026-11-10 14:45");
        Incident i106 = incident("airspace", "INC-106", "PK-217", "Karachi ACC sector KHI-2", 5, a320b, tariq,
                Map.of("separationNm", "0.8"),
                "2026-11-12 09:05", IncidentStatus.ESCALATED, null);
        i106.restoreEvidence("INC-106-E1", "Controller issued emergency avoiding action; separation 0.8 nm and 400 ft", "Capt. Tariq Mahmood", "2026-11-12 09:30");

        // Newly reported, waiting for the admin to review
        Incident i107 = incident("technical", "INC-107", "PK-851", "Islamabad (OPIS)", 2, a320c, imran,
                Map.of("aircraftSystem", "Avionics", "grounded", "false"),
                "2026-11-13 18:30", IncidentStatus.REPORTED, null);
        i107.restoreEvidence("INC-107-E1", "Intermittent FMS fault message on approach; cleared by reset", "Capt. Imran Siddiqui", "2026-11-13 18:50");
        Incident i108 = incident("weather", "INC-108", "PK-306", "Peshawar (OPPS)", 3, a320a, zainab,
                Map.of("weatherType", "Thunderstorm", "turbulenceLevel", "3"),
                "2026-11-14 12:15", IncidentStatus.REPORTED, null);
        Incident i109 = incident("technical", "INC-109", "PK-310", "Lahore (OPLA)", 4, b777c, zainab,
                Map.of("aircraftSystem", "Flight Controls", "grounded", "true"),
                "2026-11-15 04:50", IncidentStatus.REPORTED, null);
        i109.restoreEvidence("INC-109-E1", "Left aileron trim indication disagreed with actual position on pre-flight checks", "F/O Zainab Malik", "2026-11-15 05:10");

        for (Incident i : new Incident[]{i101, i102, i103, i104, i105, i106, i107, i108, i109}) {
            incidentRepo.save(i);
        }
    }

    /** Builds an incident through the Factory, then puts back its saved status and time. */
    private static Incident incident(String type, String id, String flight, String location, int severity,
                                     Aircraft aircraft, Pilot pilot, Map<String, String> extra,
                                     String reportedAt, IncidentStatus status, Investigator investigator) {
        Incident incident = IncidentFactory.create(type, id, flight, location, severity, aircraft, pilot, extra);
        incident.restoreState(reportedAt, status, investigator);
        return incident;
    }
}
