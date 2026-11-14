# ASIMS — Aviation Safety Incident Management System

CT-263 Object-Oriented Analysis & Design project. Pilots report safety incidents. The system scores the risk and notifies the right people, and investigators then follow the incident up until it is closed.

## How to run
You need **Java 17 or newer**. Check with `java -version`. You also need internet the very first time, so Maven can download itself.

| What | Command (Windows) | Mac/Linux |
|---|---|---|
| Start the app | double-click `run.bat`, or `mvnw.cmd -q compile exec:java` | `./mvnw -q compile exec:java` |
| Open it | http://localhost:8000 | same |
| Run tests | `mvnw.cmd test` | `./mvnw test` |
| Stop | `Ctrl + C` in the terminal | same |

Data lives in memory, so restarting resets it to the 3 sample incidents.

## Project structure
```
src/main/java/com/asims/
├── Main.java              starts everything, loads sample data
├── model/                 the domain classes (incidents, staff, fleet)
├── pattern/               the 4 design patterns
└── web/WebServer.java     connects the browser to the Java code
src/main/resources/web/index.html   the dashboard
src/test/java/com/asims/AsimsTest.java
docs/                      UML diagrams, test results, report draft
```

## Viva cheat-sheet: "show me where..."
| Concept | File | What to say |
|---|---|---|
| **Encapsulation** | `model/Incident.java` → `setSeverity()` | "All fields are private. Severity can only change through `setSeverity()`, which rejects values outside 1–5." |
| **Abstraction** | `model/Incident.java`, `model/StaffMember.java` | "These are abstract, so you can't create a plain Incident. They define *what* every incident must do, not *how*." |
| **Inheritance** | `TechnicalIncident extends Incident`, `Pilot extends StaffMember` | "Subclasses reuse id, flight number, evidence etc. from the parent." |
| **Polymorphism** | `calculateRiskScore()` in the 3 incident classes | "Same method call, different formula depending on the actual object type." |
| **Composition** | `Incident.addEvidence()`, `EvidenceLog` constructor | "Only the incident can create its evidence (package-private constructor). Delete the incident and the evidence goes too." |
| **Aggregation** | `model/Fleet.java` | "Aircraft are created outside the fleet. Removing the fleet doesn't destroy them." |
| **Association** | `Incident` fields `aircraft`, `reportedBy`, `assignedInvestigator` | "An incident *refers to* a pilot and an investigator, but doesn't own them." |
| **Singleton** | `pattern/IncidentRegistry.java` | "Private constructor plus `getInstance()` means there is only one registry." |
| **Factory** | `pattern/IncidentFactory.java` | "The web sends 'technical' and the factory decides which class to create." |
| **Observer** | `IncidentObserver`, `AuthorityNotifier`, `MaintenanceNotifier` | "The service notifies all subscribers. A new listener is just a new class." |
| **Facade** | `pattern/IncidentService.java` | "One simple front door. The web layer never sees the registry, factory or observers." |

### SOLID in one line each
- **S**ingle responsibility: `Fleet` manages aircraft, `IncidentRegistry` stores incidents, and `WebServer` only handles HTTP.
- **O**pen/closed: a new observer is just a new class, and the service doesn't change.
- **L**iskov substitution: any Incident subclass works wherever an `Incident` is expected. Test T4 checks this.
- **I**nterface segregation: `IncidentObserver` is tiny, with only 3 methods.
- **D**ependency inversion: `IncidentService` depends on the `IncidentObserver` interface, not on the concrete notifiers.

## Demo script (about 3 minutes)
1. Open the dashboard. Point out the 3 sample incidents and that each type has a different risk formula (**polymorphism**).
2. Report a new **Technical** incident with "Grounded = yes". It appears in the table (**Factory**). Both observer panels get a message (**Observer**).
3. Report an **Airspace** incident with separation 0.5 nm. It gets a critical risk score.
4. Try severity 9. The error message shows **encapsulation** working.
5. Click an incident, assign an investigator, and add evidence. Then **Delete** it: the evidence disappears with it (**composition**). The fleet still lists the aircraft (**aggregation**).
6. Click **Escalate** on an incident. The Aviation Authority panel gets an ESCALATED message.
7. Show `IncidentService.java`: the web server only ever calls this class (**Facade**).
