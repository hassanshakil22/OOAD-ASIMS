# ASIMS — UML Diagrams

All 8 diagrams are written in **PlantUML**, using the exact class, field and method names from the Java code in `src/main/java/com/asims`.

**How to turn them into images:**
1. Copy a code block into https://www.plantuml.com/plantuml (or use the PlantUML plugin in IntelliJ / VS Code).
2. Export it as PNG for the report.

---

## 1. Use Case Diagram

**Actors:**
- **Pilot**: reports incidents and adds evidence.
- **Safety Investigator**: investigates, escalates and closes incidents.
- **Aviation Authority** and **Maintenance Team**: external actors that receive notifications. This is the Observer pattern.

**Relationships:**
- `<<include>>`: a step that always happens.
- `<<extend>>`: a step that only happens under a condition.

```plantuml
@startuml
left to right direction
skinparam packageStyle rectangle

actor Pilot
actor "Safety Investigator" as Investigator
actor "Aviation Authority" as Authority
actor "Maintenance Team" as Maintenance

rectangle "ASIMS" {
  usecase "Report Incident" as UC1
  usecase "Calculate Risk Score" as UC2
  usecase "Notify Authority" as UC3
  usecase "Notify Maintenance" as UC4
  usecase "Add Evidence" as UC5
  usecase "View Incidents" as UC6
  usecase "Assign Investigator" as UC7
  usecase "Escalate Incident" as UC8
  usecase "Close Incident" as UC9
  usecase "Delete Incident" as UC10
}

Pilot --> UC1
Pilot --> UC5
Pilot --> UC6
Investigator --> UC5
Investigator --> UC6
Investigator --> UC7
Investigator --> UC8
Investigator --> UC9
Investigator --> UC10

UC1 ..> UC2 : <<include>>
UC1 ..> UC3 : <<include>>
UC8 ..> UC3 : <<include>>
UC4 ..> UC1 : <<extend>>\n[aircraft grounded]

UC3 --> Authority
UC4 --> Maintenance
@enduml
```

---

## 2. Activity Diagram: Report and handle an incident

**Swimlanes:** Pilot, ASIMS System and Safety Investigator.

**Notation:**
- **Fork/join bar:** the observers are notified "in parallel".
  - This is conceptual. In the code, `notifyObservers()` loops over them one after another.
- **Decision diamonds:**
  - Is the severity valid?
  - Does the aircraft need maintenance?
  - Is escalation needed?

```plantuml
@startuml
|Pilot|
start
:Fill in incident form\n(type, flight, severity, aircraft);
:Submit report;

|ASIMS System|
if (Severity between 1 and 5?) then (no)
  :Show error message;
  stop
else (yes)
endif
:IncidentFactory.create()\nbuilds correct Incident subclass;
:IncidentRegistry.add(incident);
:calculateRiskScore();
fork
  :AuthorityNotifier.update()\n"REPORTED";
fork again
  if (requiresMaintenance()?) then (yes)
    :MaintenanceNotifier.update()\n"Inspect aircraft";
  else (no)
  endif
end fork

|Safety Investigator|
:Review incident and risk level;
:assignInvestigator();
:Add evidence;
if (Risk is HIGH or CRITICAL?) then (yes)
  :escalateIncident();
  |ASIMS System|
  :AuthorityNotifier.update()\n"ESCALATED";
  |Safety Investigator|
else (no)
endif
:closeIncident();
stop
@enduml
```

---

## 3. Class Diagram

**Arrow notation:**
- **Filled diamond (`*--`):** composition. Incident → EvidenceLog.
- **Hollow diamond (`o--`):** aggregation. Fleet → Aircraft, IncidentRegistry → Incident.
- **Plain arrow:** association.
- **Triangle:** inheritance.
- **Dashed triangle:** implements an interface.

```plantuml
@startuml
skinparam classAttributeIconSize 0
hide empty members

package model {
  enum IncidentStatus {
    REPORTED
    UNDER_INVESTIGATION
    ESCALATED
    CLOSED
  }

  abstract class Incident {
    - id : String
    - flightNumber : String
    - location : String
    - reportedAt : String
    - severity : int
    - status : IncidentStatus
    - aircraft : Aircraft
    - reportedBy : Pilot
    - assignedInvestigator : Investigator
    - evidenceLogs : List<EvidenceLog>
    # Incident(id, flightNumber, location, severity, aircraft, reportedBy)
    + {abstract} calculateRiskScore() : int
    + {abstract} getType() : String
    + {abstract} getDetails() : String
    + requiresMaintenance() : boolean
    + getRiskLevel() : String
    + addEvidence(description : String, collectedBy : String) : EvidenceLog
    + assignInvestigator(investigator : Investigator) : void
    + escalate() : void
    + close() : void
    + setSeverity(severity : int) : void
    + getEvidenceLogs() : List<EvidenceLog>
  }

  class TechnicalIncident {
    - aircraftSystem : String
    - grounded : boolean
    + calculateRiskScore() : int
    + requiresMaintenance() : boolean
    + getType() : String
    + getDetails() : String
    - isCriticalSystem() : boolean
  }

  class WeatherIncident {
    - weatherType : String
    - turbulenceLevel : int
    + calculateRiskScore() : int
    + getType() : String
    + getDetails() : String
  }

  class AirspaceIncident {
    - separationNm : double
    + calculateRiskScore() : int
    + getType() : String
    + getDetails() : String
  }

  class EvidenceLog {
    - id : String
    - description : String
    - collectedBy : String
    - collectedAt : String
    ~ EvidenceLog(id, description, collectedBy)
  }

  abstract class StaffMember {
    - id : String
    - name : String
    + {abstract} getRole() : String
  }

  class Pilot {
    - rank : String
    + getRole() : String
  }

  class Investigator {
    - specialization : String
    + getRole() : String
  }

  class Aircraft {
    - registration : String
    - model : String
  }

  class Fleet {
    - airlineName : String
    - aircraftList : List<Aircraft>
    + addAircraft(aircraft : Aircraft) : void
    + removeAircraft(aircraft : Aircraft) : void
    + findAircraft(registration : String) : Aircraft
  }
}

package pattern {
  class IncidentRegistry <<Singleton>> {
    - {static} instance : IncidentRegistry
    - incidents : Map<String, Incident>
    - nextNumber : int
    - IncidentRegistry()
    + {static} getInstance() : IncidentRegistry
    + generateId() : String
    + add(incident : Incident) : void
    + findById(id : String) : Incident
    + remove(id : String) : void
    + getAll() : List<Incident>
  }

  class IncidentFactory <<Factory>> {
    + {static} create(type, id, flightNumber, location, severity, aircraft, reportedBy, extra) : Incident
  }

  interface IncidentObserver <<Observer>> {
    + update(event : String, incident : Incident) : void
    + getName() : String
    + getNotifications() : List<String>
  }

  class AuthorityNotifier {
    - notifications : List<String>
  }

  class MaintenanceNotifier {
    - notifications : List<String>
  }

  class IncidentService <<Facade>> {
    - registry : IncidentRegistry
    - fleet : Fleet
    - pilots : Map<String, Pilot>
    - investigators : Map<String, Investigator>
    - observers : List<IncidentObserver>
    + addObserver(observer : IncidentObserver) : void
    - notifyObservers(event : String, incident : Incident) : void
    + reportIncident(type, flightNumber, location, severity, aircraftRegistration, pilotId, extra) : Incident
    + assignInvestigator(incidentId : String, investigatorId : String) : void
    + addEvidence(incidentId : String, description : String, collectedBy : String) : void
    + escalateIncident(incidentId : String) : void
    + closeIncident(incidentId : String) : void
    + deleteIncident(incidentId : String) : void
  }
}

package web {
  class WebServer {
    - service : IncidentService
    + start(port : int) : void
  }
}

' Inheritance
Incident <|-- TechnicalIncident
Incident <|-- WeatherIncident
Incident <|-- AirspaceIncident
StaffMember <|-- Pilot
StaffMember <|-- Investigator
IncidentObserver <|.. AuthorityNotifier
IncidentObserver <|.. MaintenanceNotifier

' Composition and aggregation
Incident "1" *-- "0..*" EvidenceLog : owns >
Fleet "1" o-- "0..*" Aircraft : contains >
IncidentRegistry "1" o-- "0..*" Incident : stores >

' Associations
Incident "0..*" --> "1" Aircraft : involves >
Pilot "1" <-- "0..*" Incident : reportedBy
Investigator "0..1" <-- "0..*" Incident : assignedInvestigator
Incident --> IncidentStatus

' Pattern wiring
IncidentService --> "1" IncidentRegistry
IncidentService --> "1" Fleet
IncidentService --> "0..*" IncidentObserver : notifies >
IncidentService ..> IncidentFactory : <<use>>
IncidentFactory ..> TechnicalIncident : <<create>>
IncidentFactory ..> WeatherIncident : <<create>>
IncidentFactory ..> AirspaceIncident : <<create>>
WebServer --> "1" IncidentService
@enduml
```

---

## 4. Object Diagram: snapshot right after startup

This is the state built by `Main.loadSampleIncidents()`.

```plantuml
@startuml
object "registry : IncidentRegistry" as registry
object "service : IncidentService" as service
object "fleet : Fleet" as fleet {
  airlineName = "Pakistan International Airlines"
}
object "ap_bxk : Aircraft" as ap_bxk {
  registration = "AP-BXK"
  model = "Boeing 777-300ER"
}
object "p1 : Pilot" as p1 {
  id = "P1"
  name = "Capt. Tariq Mahmood"
  rank = "Captain"
}
object "i1 : Investigator" as i1 {
  id = "I1"
  name = "Sara Ahmed"
  specialization = "Aircraft Systems"
}
object "inc101 : TechnicalIncident" as inc101 {
  id = "INC-101"
  flightNumber = "PK-304"
  location = "Karachi (OPKC)"
  severity = 4
  status = UNDER_INVESTIGATION
  aircraftSystem = "Hydraulics"
  grounded = true
}
object "e1 : EvidenceLog" as e1 {
  id = "INC-101-E1"
  description = "Hydraulic system B pressure dropped..."
}
object "e2 : EvidenceLog" as e2 {
  id = "INC-101-E2"
  description = "Fluid leak found near left main gear"
}
object "authority : AuthorityNotifier" as authority
object "maintenance : MaintenanceNotifier" as maintenance

service --> registry
service --> fleet
service --> authority
service --> maintenance
fleet o-- ap_bxk
registry o-- inc101
inc101 *-- e1
inc101 *-- e2
inc101 --> ap_bxk : aircraft
inc101 --> p1 : reportedBy
inc101 --> i1 : assignedInvestigator
@enduml
```

---

## 5. Component Diagram

**Notation:**
- **Lollipop:** an interface a component *provides*.
- **Arrow into the lollipop:** another component *requires* that interface.

```plantuml
@startuml
component "Web Dashboard\n(index.html)" as UI
component "WebServer" as WEB
component "IncidentService\n<<Facade>>" as FACADE
component "IncidentRegistry\n<<Singleton>>" as REG
component "IncidentFactory\n<<Factory>>" as FAC
component "Notifiers\n(AuthorityNotifier,\nMaintenanceNotifier)" as OBS
component "Domain Model\n(Incident, StaffMember,\nFleet, Aircraft, EvidenceLog)" as MODEL

interface "REST / JSON API" as API
interface "Incident operations" as OPS
interface "IncidentObserver" as IOBS

WEB - API
UI ..> API : HTTP
FACADE - OPS
WEB ..> OPS
OBS - IOBS
FACADE ..> IOBS : notify
FACADE ..> REG
FACADE ..> FAC
FAC ..> MODEL
REG ..> MODEL
@enduml
```

---

## 6. Sequence Diagram: Report a grounded technical incident, then escalate it

```plantuml
@startuml
actor Pilot
participant "Browser\n(index.html)" as UI
participant ":WebServer" as WEB
participant ":IncidentService" as SVC
participant ":Fleet" as FLEET
participant ":IncidentRegistry" as REG
participant "IncidentFactory" as FAC
participant "inc : TechnicalIncident" as INC
participant ":AuthorityNotifier" as AUTH
participant ":MaintenanceNotifier" as MAINT

== Report incident ==
Pilot -> UI : fill form, click "Report"
activate UI
UI -> WEB : POST /api/report
activate WEB
WEB -> SVC : reportIncident("technical", ...)
activate SVC
SVC -> FLEET : findAircraft("AP-BXK")
activate FLEET
FLEET --> SVC : aircraft
deactivate FLEET
SVC -> REG : generateId()
activate REG
REG --> SVC : "INC-104"
deactivate REG
SVC -> FAC : create("technical", ...)
activate FAC
create INC
FAC -> INC : new TechnicalIncident(...)
FAC --> SVC : inc
deactivate FAC
SVC -> REG : add(inc)
activate REG
deactivate REG
SVC -> SVC : notifyObservers("REPORTED", inc)
SVC -> AUTH : update("REPORTED", inc)
activate AUTH
AUTH -> INC : calculateRiskScore()
INC --> AUTH : 88
deactivate AUTH
SVC -> MAINT : update("REPORTED", inc)
activate MAINT
MAINT -> INC : requiresMaintenance()
INC --> MAINT : true
deactivate MAINT
SVC --> WEB : inc
deactivate SVC
WEB --> UI : {"ok": true}
deactivate WEB
UI --> Pilot : updated incident table
deactivate UI

== Escalate incident ==
Pilot -> UI : click "Escalate"
activate UI
UI -> WEB : POST /api/escalate
activate WEB
WEB -> SVC : escalateIncident("INC-104")
activate SVC
SVC -> REG : findById("INC-104")
activate REG
REG --> SVC : inc
deactivate REG
SVC -> INC : escalate()
SVC -> SVC : notifyObservers("ESCALATED", inc)
SVC -> AUTH : update("ESCALATED", inc)
activate AUTH
deactivate AUTH
SVC -> MAINT : update("ESCALATED", inc)
activate MAINT
deactivate MAINT
SVC --> WEB : (void)
deactivate SVC
WEB --> UI : {"ok": true}
deactivate WEB
UI --> Pilot : status shows ESCALATED
deactivate UI
@enduml
```

---

## 7. Communication Diagram (same scenario as the sequence diagram)

This shows the same messages as the sequence diagram, but arranged around the **links between objects** instead of along a timeline. The message numbers give the order. PlantUML has no dedicated communication-diagram type, so it is drawn with objects and numbered labels on the links.

```plantuml
@startuml
left to right direction
object "ui : Browser" as UI
object ":WebServer" as WEB
object ":IncidentService" as SVC
object ":Fleet" as FLEET
object ":IncidentRegistry" as REG
object "IncidentFactory" as FAC
object "inc : TechnicalIncident" as INC
object ":AuthorityNotifier" as AUTH
object ":MaintenanceNotifier" as MAINT

UI --> WEB : 1: POST /api/report\n2: POST /api/escalate
WEB --> SVC : 1.1: reportIncident(...)\n2.1: escalateIncident(id)
SVC --> FLEET : 1.1.1: findAircraft(reg)
SVC --> REG : 1.1.2: generateId()\n1.1.4: add(inc)\n2.1.1: findById(id)
SVC --> FAC : 1.1.3: create(type, ...)
FAC --> INC : 1.1.3.1: <<create>>
SVC --> INC : 2.1.2: escalate()
SVC --> AUTH : 1.1.5: update("REPORTED", inc)\n2.1.3: update("ESCALATED", inc)
SVC --> MAINT : 1.1.6: update("REPORTED", inc)\n2.1.4: update("ESCALATED", inc)
AUTH --> INC : 1.1.5.1: calculateRiskScore()
MAINT --> INC : 1.1.6.1: requiresMaintenance()
@enduml
```

---

## 8. Deployment Diagram

**Nodes:**
- **Client workstation:** runs a web browser.
- **Application server:** runs the Java program (JVM 17+).
- **Storage:** data is kept **in memory** inside the JVM. There is no separate database server.
- **External notifications:** simulated inside the app. The notifiers store messages instead of contacting real external systems.

```plantuml
@startuml
node "Client Workstation" as client {
  node "Web Browser" {
    artifact "index.html\n(HTML/CSS/JS)" as html
  }
}

node "Application Server" as server {
  node "Java Runtime (JVM 17+)" {
    artifact "asims-1.0 classes\n(com.asims.*)" as app
    artifact "gson-2.11.0.jar" as gson
    database "In-memory store\n(IncidentRegistry)" as mem
  }
}

client -- server : HTTP / REST (JSON)\nlocalhost:8000
app ..> gson
app ..> mem
@enduml
```
