package com.asims;

import com.asims.model.Aircraft;
import com.asims.model.Fleet;
import com.asims.model.Incident;
import com.asims.model.Investigator;
import com.asims.model.Pilot;
import com.asims.pattern.AuthorityNotifier;
import com.asims.pattern.IncidentRegistry;
import com.asims.pattern.IncidentService;
import com.asims.pattern.MaintenanceNotifier;
import com.asims.web.WebServer;

import java.util.Map;

/**
 * Starting point of the program.
 *  1. Creates the objects and connects them together.
 *  2. Loads some sample data so the demo isn't empty.
 *  3. Starts the web server on http://localhost:8000
 */
public class Main {

    public static void main(String[] args) throws Exception {
        // AGGREGATION: aircraft are created here, OUTSIDE the fleet, then added to it.
        Fleet fleet = new Fleet("Pakistan International Airlines");
        fleet.addAircraft(new Aircraft("AP-BXK", "Boeing 777-300ER"));
        fleet.addAircraft(new Aircraft("AP-BLW", "Airbus A320-200"));
        fleet.addAircraft(new Aircraft("AP-BKT", "ATR 72-500"));

        // SINGLETON: there is only one registry, fetched with getInstance().
        IncidentService service = new IncidentService(IncidentRegistry.getInstance(), fleet);

        // OBSERVER: subscribe the listeners.
        service.addObserver(new AuthorityNotifier());
        service.addObserver(new MaintenanceNotifier());

        // Staff
        service.addPilot(new Pilot("P1", "Capt. Tariq Mahmood", "Captain"));
        service.addPilot(new Pilot("P2", "F/O Hina Raza", "First Officer"));
        service.addInvestigator(new Investigator("I1", "Sara Ahmed", "Aircraft Systems"));
        service.addInvestigator(new Investigator("I2", "Omar Farooq", "Air Traffic & Human Factors"));

        loadSampleIncidents(service);

        new WebServer(service).start(8000);
    }

    private static void loadSampleIncidents(IncidentService service) {
        Incident technical = service.reportIncident("technical", "PK-304", "Karachi (OPKC)", 4,
                "AP-BXK", "P1", Map.of("aircraftSystem", "Hydraulics", "grounded", "true"));
        service.addEvidence(technical.getId(), "Hydraulic system B pressure dropped to 0 psi on climb", "Capt. Tariq Mahmood");
        service.addEvidence(technical.getId(), "Fluid leak found near left main landing gear", "Sara Ahmed");
        service.assignInvestigator(technical.getId(), "I1");

        Incident weather = service.reportIncident("weather", "PK-731", "Near Lahore (OPLA)", 3,
                "AP-BKT", "P2", Map.of("weatherType", "Severe Turbulence", "turbulenceLevel", "4"));
        service.addEvidence(weather.getId(), "Two cabin crew injured, seatbelt sign was on", "F/O Hina Raza");

        service.reportIncident("airspace", "PK-217", "Karachi sector KHI-2", 3,
                "AP-BLW", "P1", Map.of("separationNm", "2.1"));
    }
}
