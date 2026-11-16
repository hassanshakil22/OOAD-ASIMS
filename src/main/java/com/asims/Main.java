package com.asims;

import com.asims.auth.AuthService;
import com.asims.model.Fleet;
import com.asims.pattern.AdminNotifier;
import com.asims.pattern.AuthorityNotifier;
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
import com.asims.web.WebServer;

import java.nio.file.Path;

/**
 * Starting point of the program.
 *  1. Opens the SQLite database (data/asims.db) and fills it with starting data on the first run.
 *  2. Loads the fleet, staff and incidents from the database into objects.
 *  3. Connects the objects together and starts the web server on http://localhost:8000
 */
public class Main {

    public static void main(String[] args) throws Exception {
        // ----- Database -----
        Database database = Database.openFile(Path.of("data", "asims.db"));
        AircraftRepository aircraftRepo = new AircraftRepository(database);
        StaffRepository staffRepo = new StaffRepository(database);
        AccountRepository accountRepo = new AccountRepository(database);
        IncidentRepository incidentRepo = new IncidentRepository(database);
        SeedData.seedIfEmpty(aircraftRepo, staffRepo, accountRepo, incidentRepo);

        // AGGREGATION: the aircraft are loaded from the database, OUTSIDE the fleet, then added to it.
        Fleet fleet = new Fleet("Pakistan International Airlines");
        aircraftRepo.loadAll().forEach(fleet::addAircraft);

        // SINGLETON: there is only one registry, fetched with getInstance().
        IncidentService service = new IncidentService(IncidentRegistry.getInstance(), fleet, incidentRepo);

        // OBSERVER: subscribe the listeners.
        service.addObserver(new AdminNotifier());
        service.addObserver(new IncidentManagementNotifier());
        service.addObserver(new AuthorityNotifier());
        service.addObserver(new MaintenanceNotifier());

        // Staff and saved incidents
        staffRepo.loadPilots().forEach(service::addPilot);
        staffRepo.loadInvestigators().forEach(service::addInvestigator);
        service.loadFromDatabase();
        service.rebuildInboxes();

        new WebServer(service, new AuthService(accountRepo)).start(8000);
    }
}
