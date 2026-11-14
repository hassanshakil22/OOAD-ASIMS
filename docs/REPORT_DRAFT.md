# Aviation Safety Incident Management System (ASIMS)
### CT-263 Object-Oriented Analysis & Design — Complex Computing Problem Report (Draft)

> **Draft notes for the group:**
> - Replace the `[...]` placeholders.
> - Export the diagrams from `docs/UML_DIAGRAMS.md` as images and insert them where marked.
> - Rewrite the text in your own words before submitting.

---

## 1. Introduction
Airlines and air navigation providers must record every safety-related event, from a hydraulic failure to two aircraft passing too close. Each event has to be assessed for risk, investigated and reported to the national aviation authority. In many smaller operators this is still done with paper forms, spreadsheets and email. Reports get lost, risk is judged inconsistently, and the authority is often informed late.

ASIMS is a small web-based system that supports this process:
- Pilots report incidents.
- The system calculates a risk score and notifies the relevant parties automatically.
- Investigators assign themselves, collect evidence, escalate the incident if needed, and finally close it.

The project's main purpose is to apply object-oriented analysis and design: the four OOP pillars, class relationships, SOLID principles, design patterns and UML modelling.

## 2. Objectives
1. Model the incident-reporting domain with UML: use case, activity, class, object, component, sequence, communication and deployment diagrams.
2. Implement the design in Java using **encapsulation, abstraction, inheritance and polymorphism**.
3. Show the three class relationships: **association, aggregation and composition**.
4. Apply four GoF design patterns:
   - **Singleton** and **Factory** (creational)
   - **Facade** (structural)
   - **Observer** (behavioural)
5. Follow the **SOLID** principles so the system is easy to extend.
6. Verify the system with automated unit tests.

## 3. Problem Statement
A safety department needs a single system in which:
- **Different kinds of incident** (technical, weather and airspace) are reported in one place. Each kind has its own data and its own way of measuring risk.
- **Every incident has one clear status** (reported, under investigation, escalated, closed), and only valid changes are allowed.
- **Interested parties are informed automatically.** The aviation authority hears about every new or escalated incident. The maintenance team hears about every grounded aircraft.
- **Evidence stays attached** to the incident it belongs to.
- **New incident types or new notification receivers** can be added later without rewriting existing code.

## 4. Methodology

### 4.1 Approach
We followed an iterative object-oriented process:
1. **Requirements:** identify the actors and use cases.
2. **Analysis:** find the domain classes and their relationships.
3. **Design:** choose patterns and draw the class, sequence and other diagrams.
4. **Implementation:** write the code in Java 17 with Maven.
5. **Testing:** write JUnit 5 unit tests and run manual system tests.

### 4.2 Technology
- **Backend:** Java 17+, using the JDK's built-in HTTP server.
- **Frontend:** a single HTML/CSS/JavaScript page that talks to the backend through a JSON REST API.
- **Libraries:** Gson for JSON.
- **Build and tests:** Maven, JUnit 5.

We deliberately avoided large frameworks so that every pattern is visible in our own code.

### 4.3 UML Diagrams
*(Insert each exported diagram image below its heading.)*

1. **Use Case Diagram.**
   - Actors: Pilot, Safety Investigator, and the external Aviation Authority and Maintenance Team.
   - "Report Incident" always *includes* "Calculate Risk Score" and "Notify Authority".
   - "Notify Maintenance" *extends* "Report Incident" only when the aircraft is grounded.
2. **Activity Diagram.**
   - Follows an incident from the form to closure, across three swimlanes: Pilot, System and Investigator.
   - Decision nodes check whether the severity is valid and whether escalation is needed.
   - A fork/join shows the observers being notified.
3. **Class Diagram.**
   - Inheritance: `Incident` → 3 subclasses, and `StaffMember` → `Pilot` / `Investigator`.
   - Composition: `Incident` ◆— `EvidenceLog`.
   - Aggregation: `Fleet` ◇— `Aircraft`.
   - Associations: an incident's reporter, investigator and aircraft.
   - Pattern classes are marked with stereotypes.
4. **Object Diagram.**
   - A snapshot of the running system after start-up.
   - Incident `INC-101` (PK-304, hydraulic failure, grounded) with its two evidence logs, pilot, investigator and aircraft.
5. **Component Diagram.**
   - Web Dashboard → WebServer (provides the REST API) → IncidentService facade (provides incident operations).
   - The facade in turn uses the Registry, the Factory, the Notifiers (through the `IncidentObserver` interface) and the Domain Model.
6. **Sequence Diagram.**
   - The time-ordered messages for reporting a grounded technical incident and then escalating it.
   - Shows the Factory creating the object, the Registry storing it, and both observers being notified.
7. **Communication Diagram.**
   - The same scenario as the sequence diagram, drawn around object links.
   - Numbered messages (1, 1.1, 1.1.1 …) show the order.
8. **Deployment Diagram.**
   - A client workstation running a browser connects over HTTP to an application server running the JVM.
   - Data is held in memory.

### 4.4 Object-Oriented Principles
| Principle | How ASIMS applies it |
|---|---|
| Encapsulation | All fields are `private`. `setSeverity()` validates 1–5. The evidence list is returned read-only. |
| Abstraction | `Incident` and `StaffMember` are abstract classes that define common behaviour. |
| Inheritance | `TechnicalIncident`, `WeatherIncident` and `AirspaceIncident` extend `Incident`. `Pilot` and `Investigator` extend `StaffMember`. |
| Polymorphism | `calculateRiskScore()`, `getType()`, `getDetails()` and `requiresMaintenance()` are overridden per type. |
| Association | `Incident` → `Pilot` (reportedBy), `Investigator` (assignedInvestigator), `Aircraft`. |
| Aggregation | `Fleet` holds aircraft created elsewhere. They survive removal from the fleet. |
| Composition | `Incident` alone creates its `EvidenceLog`s (package-private constructor). They are deleted with it. |

### 4.5 Design Patterns
| Pattern | Type | Class | Problem solved |
|---|---|---|---|
| Singleton | Creational | `IncidentRegistry` | Guarantees one shared list of incidents (private constructor, `getInstance()`). |
| Factory | Creational | `IncidentFactory` | Creates the correct subclass from a type name, so clients never use `new` on subclasses. |
| Facade | Structural | `IncidentService` | Gives the web layer one simple interface and hides the registry, factory and observers. |
| Observer | Behavioural | `IncidentObserver`, `AuthorityNotifier`, `MaintenanceNotifier` | Notifies any number of listeners automatically when incidents are reported or escalated. |

### 4.6 SOLID Principles
| Principle | Example |
|---|---|
| Single Responsibility | `Fleet` manages aircraft, `IncidentRegistry` stores incidents, and `WebServer` handles HTTP only. |
| Open/Closed | A new notification receiver is a new class implementing `IncidentObserver`, and no existing code changes. |
| Liskov Substitution | Any `Incident` subclass can be used through an `Incident` reference (test T4). |
| Interface Segregation | `IncidentObserver` contains only the three methods observers need. |
| Dependency Inversion | `IncidentService` depends on the `IncidentObserver` abstraction, not on concrete notifiers. |

## 5. Results
- The system runs as a web application on `http://localhost:8000`. It supports reporting, assigning, adding evidence, escalating, closing and deleting incidents. *[Insert dashboard screenshots]*
- Risk scores differ by incident type. For example, at severity 3:
  - a grounded engine failure scores 76;
  - turbulence level 2 scores 50;
  - a 0.5 nm separation loss scores 80.
- Observers react automatically:
  - The aviation authority receives every REPORTED and ESCALATED event.
  - The maintenance team receives only grounded-aircraft events.
- Invalid operations are rejected with clear messages. Examples are a severity of 9, or closing an incident before an investigator is assigned.
- **All 11 automated JUnit tests pass**, covering each OOP concept and pattern. See `docs/TEST_RESULTS.md` for the full test-case table.

## 6. Conclusion
ASIMS shows how object-oriented analysis and design produce a system that is easy to understand and extend:
- **Abstraction and polymorphism** let each incident type define its own risk logic behind a common interface.
- **Composition and aggregation** model real-world ownership correctly.
- **The four design patterns** each solve a concrete problem: one registry, flexible object creation, a simple entry point and automatic notifications.

UML modelling before coding kept the code and the diagrams consistent.

**Limitations and future work:**
- Data is kept in memory only. A database could be added behind a repository interface.
- There is no login or role-based access.
- Notifications are simulated rather than sent by email or SMS.
- More incident types (e.g. ground operations) can be added as new subclasses without changing existing code.

## References
- Gamma, E., Helm, R., Johnson, R., Vlissides, J. *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley, 1994.
- ICAO Annex 13: *Aircraft Accident and Incident Investigation*.
- ICAO Doc 9859: *Safety Management Manual*.
- [Course textbook / lecture notes]
