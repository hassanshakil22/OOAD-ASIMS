# ASIMS — Aviation Safety Incident Management System (Simple Version)

## Goal
A **small, easy-to-explain** Java project for CT-263 OOAD:
- **CCA:** a working demo showing the 4 OOP pillars, class relationships, some SOLID principles and 4 design patterns.
- **CCP:** 8 UML diagrams that match the code exactly.

Rule: **if we can't explain it in one sentence, we don't build it.**

## Tech stack
| Part | Tool |
|---|---|
| Language | Java 25 (already installed) |
| Build | Maven via `mvnw` (downloads itself) |
| Web server | Java's built-in `HttpServer` (one file) |
| JSON | Gson |
| Tests | JUnit 5 |
| Frontend | One `index.html` page |
| Storage | In memory (no database, no files) |

Run: `run.bat`, then open http://localhost:8000. Tests: `mvnw.cmd test`.

## Classes (~19 files)
```
src/main/java/com/asims/
├── Main.java                         starts everything + loads sample data
├── model/
│   ├── IncidentStatus.java           enum: REPORTED, UNDER_INVESTIGATION, ESCALATED, CLOSED
│   ├── Incident.java                 ABSTRACT parent of all incidents
│   ├── TechnicalIncident.java        e.g. engine/hydraulic failure
│   ├── WeatherIncident.java          e.g. severe turbulence
│   ├── AirspaceIncident.java         e.g. two planes too close
│   ├── EvidenceLog.java              owned by an Incident (composition)
│   ├── StaffMember.java              ABSTRACT parent of all staff
│   ├── Pilot.java                    reports incidents
│   ├── Investigator.java             investigates incidents
│   ├── Aircraft.java                 a plane
│   └── Fleet.java                    list of aircraft (aggregation)
├── pattern/
│   ├── IncidentRegistry.java         SINGLETON — one central list of incidents
│   ├── IncidentFactory.java          FACTORY — creates the right Incident subclass
│   ├── IncidentObserver.java         OBSERVER interface
│   ├── AuthorityNotifier.java        observer: alerts the aviation authority
│   ├── MaintenanceNotifier.java      observer: alerts maintenance for grounded aircraft
│   └── IncidentService.java          FACADE — the one class the web page talks to
└── web/WebServer.java                handles browser requests
src/main/resources/web/index.html
src/test/java/com/asims/AsimsTest.java
```

## Where each concept lives
| Concept | Where |
|---|---|
| **Encapsulation** | All fields `private`, accessed via getters; `setSeverity()` rejects values outside 1–5 |
| **Abstraction** | `Incident` and `StaffMember` are `abstract` classes |
| **Inheritance** | 3 incident types extend `Incident`; `Pilot` and `Investigator` extend `StaffMember` |
| **Polymorphism** | Each incident type overrides `calculateRiskScore()` differently |
| **Composition** | `Incident` creates and owns its `EvidenceLog`s — delete the incident, evidence is gone |
| **Aggregation** | `Fleet` holds `Aircraft` created elsewhere — delete the fleet, aircraft still exist |
| **Association** | `Pilot` reports an `Incident`; `Investigator` is assigned to an `Incident` |
| **Singleton** | `IncidentRegistry.getInstance()` |
| **Factory** | `IncidentFactory.create("technical", ...)` |
| **Observer** | `IncidentService` notifies `AuthorityNotifier` + `MaintenanceNotifier` on new/escalated incidents |
| **Facade** | `IncidentService` hides registry + factory + observers behind simple methods |
| **SOLID** | S: each class one job · O: new incident type = new subclass, no edits · L: any subclass works as `Incident` · I: tiny `IncidentObserver` interface · D: service depends on the `IncidentObserver` interface, not concrete notifiers |

## Key classes in brief
- **`Incident` (abstract):**
  - Fields: `id, flightNumber, location, severity (1–5), status, aircraft, reportedBy, assignedInvestigator, evidenceLogs`.
  - Methods: `addEvidence()`, `assignInvestigator()`, `escalate()`, `close()`.
  - Abstract methods: `calculateRiskScore()`, `getType()`, `getDetails()`.
  - Overridable: `requiresMaintenance()` (TechnicalIncident returns `grounded`). Shared: `getRiskLevel()`.
- **Subclasses:**
  - `TechnicalIncident`: `aircraftSystem`, `isGrounded`.
  - `WeatherIncident`: `weatherType`, `turbulenceLevel`.
  - `AirspaceIncident`: `separationNm`.
- **`IncidentService`:** `reportIncident()`, `assignInvestigator()`, `addEvidence()`, `escalateIncident()`, `closeIncident()`, `deleteIncident()`, `getAllIncidents()`, `getObservers()`.

## Sample data
- **Pilots:** Capt. Tariq Mahmood, F/O Hina Raza.
- **Investigators:** Sara Ahmed, Omar Farooq.
- **Fleet:** AP-BXK Boeing 777-300ER, AP-BLW Airbus A320, AP-BKT ATR 72.
- **Incidents:**
  - PK-304: hydraulic failure, aircraft grounded (Technical).
  - PK-731: severe turbulence (Weather).
  - PK-217: planes 2.1 nm apart (Airspace).

## Web page (one screen)
1. Form to report an incident. Choose the type, and the extra fields change to match.
2. Table of incidents with their risk score and status, plus Assign / Escalate / Close buttons.
3. Click an incident to see its evidence (composition).
4. Event log showing observer notifications as they happen.

## Tests (~8, JUnit)
- The Singleton returns the same object.
- The Factory creates the correct type.
- Each type calculates its risk score differently (polymorphism).
- An invalid severity is rejected (encapsulation).
- Observers get notified on escalation.
- The Maintenance observer only reacts to grounded aircraft.
- Evidence belongs to its incident (composition).
- An aircraft still exists after the fleet is removed (aggregation).

## Docs
- `docs/UML_DIAGRAMS.md`: all 8 diagrams (Use Case, Activity, Class, Object, Component, Sequence, Communication, Deployment) in PlantUML, using the exact class and method names from the code.
- `docs/REPORT_DRAFT.md`: Introduction, Objectives, Problem Statement, Methodology, Results (test table), Conclusion.

## Build order (pause for review after each)
1. Maven skeleton + `model` package.
2. `pattern` package.
3. Web server + page.
4. Tests.
5. UML + report.
