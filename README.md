# ASIMS — Aviation Safety Incident Management System

CT-263 Object-Oriented Analysis & Design project. Pilots report safety incidents; the **admin** reviews and **escalates** them; the **incident manager** assigns an investigator, collects evidence and closes the case. Everything is saved in a SQLite database.

## How to run
You need **Java 17 or newer** (`java -version`). You need internet the very first time, so Maven can download itself and the libraries.

| What | Command (Windows) | Mac/Linux |
|---|---|---|
| Start the app | double-click `run.bat`, or `mvnw.cmd -q compile exec:java` | `./mvnw -q compile exec:java` |
| Open it | http://localhost:8000 | same |
| Run tests | `mvnw.cmd test` | `./mvnw test` |
| Stop | `Ctrl + C` in the terminal | same |

The database file `data/asims.db` is created and filled with realistic sample data on the first run. **To reset everything**, stop the app and delete the `data` folder.

## Demo logins
| Role | Username | Password | Portal |
|---|---|---|---|
| Admin | `admin` | `admin123` | /admin |
| Incident Manager | `manager` | `manager123` | /manager |
| Pilot | `tariq`, `hina`, `imran`, `zainab` | `pilot123` | /pilot |

These are demo accounts for the lab. They are stored hashed (PBKDF2) in the database and are also shown on the login page.

## The workflow (3-minute demo script)
1. Log in as **tariq** (pilot). Report an incident (try *Technical*, grounded = yes). It shows up in "My Incident Reports" as REPORTED.
2. In a second window (or a private window), log in as **admin**. The new report is in the **inbox** with a notification. Click **Escalate**.
3. Log in as **manager**. The escalated incident is in the **case queue**. Select it, **assign an investigator**, **add evidence**, then **close the case**.
4. Back in the pilot window, the report now shows CLOSED (the progress bar moves along by itself).
5. Show the safety rules: as pilot, open `/admin` (you are sent back to your own portal); try severity 9 (error message); as admin, you cannot close a case.
6. Stop and restart the app: all data is still there.

## Project structure
```
src/main/java/com/asims/
├── Main.java                  opens the database, wires everything together, starts the server
├── model/                     domain classes (incidents, staff, fleet, evidence)
├── pattern/                   the 4 design patterns + the 4 notifiers
├── auth/                      Role, Permission, Account, PasswordHasher, AuthService
├── repository/                Database (SQLite) and one repository per table group, SeedData
└── web/WebServer.java         pages, login and the JSON API
src/main/resources/web/        login.html, pilot.html, admin.html, manager.html, common.css/js
src/test/java/com/asims/AsimsTest.java
docs/                          report (PDF + Word), UML diagrams, test results
```

## Viva cheat-sheet: "show me where..."
| Concept | File | What to say |
|---|---|---|
| **Encapsulation** | `model/Incident.java` | "All fields are private. Severity only changes through `setSeverity()` (1–5) and status only through `escalate()`, `assignInvestigator()` and `close()`, which enforce the workflow." |
| **Abstraction** | `Incident`, `StaffMember` | "Abstract: you can't create a plain Incident. They define *what* every incident must do, not *how*." |
| **Inheritance** | `TechnicalIncident extends Incident`, `Pilot extends StaffMember` | "Subclasses reuse id, flight number, evidence etc. from the parent." |
| **Polymorphism** | `calculateRiskScore()` in the 3 incident classes; `Role.canView()` | "Same call, different behaviour depending on the real object. Each role also decides for itself which incidents it can see." |
| **Composition** | `Incident.addEvidence()`, `EvidenceLog` constructor, `evidence` table | "Only the incident can create its evidence. Delete the incident and the evidence goes — in memory, and in SQL through `ON DELETE CASCADE`." |
| **Aggregation** | `model/Fleet.java` | "Aircraft are loaded outside the fleet and only added to it. Removing one from the fleet doesn't destroy it." |
| **Association** | `Incident` fields `aircraft`, `reportedBy`, `assignedInvestigator`; `Account.role` | "References to objects that live on their own." |
| **Singleton** | `pattern/IncidentRegistry.java` | "Private constructor plus `getInstance()` means there is only one registry." |
| **Factory** | `pattern/IncidentFactory.java` | "The page sends 'technical' and the factory creates the class. The database layer reuses it to rebuild the right subclass for every saved row." |
| **Observer** | `IncidentObserver` + 4 notifiers | "The service notifies all subscribers. We added the admin and incident-management notifiers without touching the service (Open/Closed)." |
| **Facade** | `pattern/IncidentService.java` | "One front door: it checks the user's permission, then uses the factory, registry, database and observers. The web layer knows none of them." |
| **Roles / login** | `auth/` package | "`Role` holds the permissions; `IncidentService` checks them on every action; passwords are salted PBKDF2 hashes; the session is an HttpOnly cookie." |
| **Database** | `repository/` package | "SQLite file. Each repository saves/loads one kind of object; `Incident.getExtraFields()` lets one JSON column hold the type-specific fields." |

### SOLID in one line each
- **S**ingle responsibility: `Fleet` manages aircraft, each repository handles its own tables, `AuthService` handles login, `WebServer` handles HTTP only.
- **O**pen/closed: a new notifier or incident type is a new class; existing classes don't change.
- **L**iskov substitution: any `Incident` subclass works wherever an `Incident` is expected (tests T4, T16).
- **I**nterface segregation: `IncidentObserver` is tiny; permissions are small separate values.
- **D**ependency inversion: `IncidentService` depends on the `IncidentObserver` interface and gets its repository through the constructor.
