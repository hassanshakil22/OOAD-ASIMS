# ASIMS — UML Diagrams

All 8 diagrams are written in **PlantUML**, using the exact class, attribute and method names from the Java code in `src/main/java/com/asims`.
Ready-made SVG images of every diagram are in the `UML_DIAGRAMS/` folder (same file names as below).

**To redraw one:** paste its code block into https://www.plantuml.com/plantuml, or use the PlantUML extension in VS Code (`Alt + D`).

---

## 1. Use Case Diagram

Actors: Pilot, Admin, Incident Manager (each with their own portal) and the external Aviation Authority and Maintenance Team. Every user must log in. `<<include>>` marks steps that always happen; `<<extend>>` marks a step that only happens under a condition (aircraft grounded).

```plantuml
@startuml
left to right direction
skinparam packageStyle rectangle

actor Pilot
actor "Admin" as Admin
actor "Incident Manager" as Manager
actor "Aviation Authority" as Authority
actor "Maintenance Team" as Maintenance

rectangle "ASIMS" {
  usecase "Log in" as UC0
  usecase "Report Incident" as UC1
  usecase "Calculate Risk Score" as UC2
  usecase "View My Reports" as UC3
  usecase "Add Evidence" as UC4
  usecase "Review Incident Inbox" as UC5
  usecase "Escalate Incident" as UC6
  usecase "Delete Incident" as UC7
  usecase "Assign Investigator" as UC8
  usecase "Close Incident" as UC9
  usecase "Notify Admin Inbox" as UC10
  usecase "Notify Authority" as UC11
  usecase "Notify Incident Management" as UC12
  usecase "Notify Maintenance" as UC13
}

Pilot --> UC0
Admin --> UC0
Manager --> UC0
Pilot --> UC1
Pilot --> UC3
Pilot --> UC4
Admin --> UC5
Admin --> UC6
Admin --> UC7
Manager --> UC8
Manager --> UC9
Manager --> UC4

UC1 ..> UC2 : <<include>>
UC1 ..> UC10 : <<include>>
UC1 ..> UC11 : <<include>>
UC6 ..> UC11 : <<include>>
UC6 ..> UC12 : <<include>>
UC13 ..> UC1 : <<extend>>\n[aircraft grounded]

UC11 --> Authority
UC13 --> Maintenance
@enduml
```

Image: `UML_DIAGRAMS/01-use-case.svg`

---

## 2. Activity Diagram: report, escalate, investigate and close an incident

Four swimlanes (Pilot, ASIMS System, Admin, Incident Manager), decision diamonds and a fork/join for the notifications.

```plantuml
@startuml
skinparam defaultFontSize 13
|Pilot|
start
:Log in, fill in and\nsubmit the incident report;

|ASIMS System|
if (Severity 1 to 5?) then (no)
  :Show error;
  stop
else (yes)
endif
:Factory creates the Incident;\ncalculateRiskScore();\nsave to Registry + Database;
fork
  :Notify admin inbox\nand authority;
fork again
  if (Aircraft grounded?) then (yes)
    :Notify maintenance;
  else (no)
  endif
end fork

|Admin|
:Review the report;
if (Escalate?) then (yes)
  |ASIMS System|
  :status = ESCALATED, save;\nnotify incident management;
  |Incident Manager|
  :Assign investigator\n(UNDER_INVESTIGATION);
  :Add evidence and findings;
  :Close the case;
  |ASIMS System|
  :status = CLOSED, save;
else (no)
  |Admin|
  :Delete the incident;
endif
stop
@enduml
```

Image: `UML_DIAGRAMS/02-activity.svg`

---

## 3a. Class Diagram, Part A: Domain Model

Classes of the `model` package: inheritance, composition (`*--`, Incident to EvidenceLog), aggregation (`o--`, Fleet to Aircraft) and associations with multiplicities.

```plantuml
@startuml
skinparam classAttributeIconSize 0
hide empty members
title Class Diagram (Part A): Domain Model — package com.asims.model

enum IncidentStatus {
  REPORTED
  ESCALATED
  UNDER_INVESTIGATION
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
  + {abstract} getExtraFields() : Map<String, String>
  + requiresMaintenance() : boolean
  + getRiskLevel() : String
  + addEvidence(description : String, collectedBy : String) : EvidenceLog
  + escalate() : void
  + assignInvestigator(investigator : Investigator) : void
  + close() : void
  + setSeverity(severity : int) : void
  + restoreState(reportedAt, status, investigator) : void
  + restoreEvidence(id, description, collectedBy, collectedAt) : void
  + getEvidenceLogs() : List<EvidenceLog>
}

class TechnicalIncident {
  - aircraftSystem : String
  - grounded : boolean
  + calculateRiskScore() : int
  + requiresMaintenance() : boolean
  + getType() : String
  + getDetails() : String
  + getExtraFields() : Map<String, String>
}

class WeatherIncident {
  - weatherType : String
  - turbulenceLevel : int
  + calculateRiskScore() : int
  + getType() : String
  + getDetails() : String
  + getExtraFields() : Map<String, String>
}

class AirspaceIncident {
  - separationNm : double
  + calculateRiskScore() : int
  + getType() : String
  + getDetails() : String
  + getExtraFields() : Map<String, String>
}

class EvidenceLog {
  - id : String
  - description : String
  - collectedBy : String
  - collectedAt : String
  ~ EvidenceLog(id, description, collectedBy)
  ~ EvidenceLog(id, description, collectedBy, collectedAt)
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

Incident <|-- TechnicalIncident
Incident <|-- WeatherIncident
Incident <|-- AirspaceIncident
StaffMember <|-- Pilot
StaffMember <|-- Investigator

Incident "1" *-left- "0..*" EvidenceLog : owns >
Fleet "1" o-- "0..*" Aircraft : contains >
Incident "0..*" --> "1" Aircraft : involves >
Incident "0..*" -right-> "1" Pilot : reportedBy >
Incident "0..*" -right-> "0..1" Investigator : assignedInvestigator >
Incident -left-> IncidentStatus
EvidenceLog -[hidden]down- IncidentStatus

together {
  abstract class StaffMember
  class Pilot
  class Investigator
}
together {
  class Fleet
  class Aircraft
}
Investigator -[hidden]down- Fleet
@enduml
```

Image: `UML_DIAGRAMS/03a-class-model.svg`

---

## 3b. Class Diagram, Part B: Design Patterns, Facade and Web Layer

The four GoF patterns with their stereotypes (Singleton, Factory, Observer, Facade) and the web server.

```plantuml
@startuml
skinparam classAttributeIconSize 0
hide empty members
title Class Diagram (Part B): Design Patterns, Facade and Web Layer

package model <<Frame>> {
  abstract class Incident
  class TechnicalIncident
  class WeatherIncident
  class AirspaceIncident
  class Fleet
}

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

class AdminNotifier
class IncidentManagementNotifier
class AuthorityNotifier
class MaintenanceNotifier

class IncidentService <<Facade>> {
  - registry : IncidentRegistry
  - fleet : Fleet
  - repository : IncidentRepository
  - observers : List<IncidentObserver>
  + addObserver(observer : IncidentObserver) : void
  - notifyObservers(event : String, incident : Incident) : void
  + loadFromDatabase() : void
  + rebuildInboxes() : void
  + reportIncident(actor : Account, type, flightNumber, location, severity, aircraftRegistration, extra) : Incident
  + escalateIncident(actor : Account, incidentId : String) : void
  + assignInvestigator(actor : Account, incidentId : String, investigatorId : String) : void
  + addEvidence(actor : Account, incidentId : String, description : String) : void
  + closeIncident(actor : Account, incidentId : String) : void
  + deleteIncident(actor : Account, incidentId : String) : void
  + getIncidentsFor(actor : Account) : List<Incident>
  - requirePermission(actor : Account, permission : Permission) : void
}

class WebServer {
  - service : IncidentService
  - auth : AuthService
  + start(port : int) : void
}

class IncidentRepository
class Account
class AuthService

IncidentObserver <|.. AdminNotifier
IncidentObserver <|.. IncidentManagementNotifier
IncidentObserver <|.. AuthorityNotifier
IncidentObserver <|.. MaintenanceNotifier

WebServer --> "1" IncidentService
WebServer --> "1" AuthService
IncidentService --> "1" IncidentRegistry
IncidentService --> "1" Fleet
IncidentService --> "1" IncidentRepository
IncidentService --> "0..*" IncidentObserver : notifies >
IncidentService ..> IncidentFactory : <<use>>
IncidentService ..> Account : <<checks role of>>
IncidentRegistry "1" o-- "0..*" Incident : stores >
IncidentFactory ..> TechnicalIncident : <<create>>
IncidentFactory ..> WeatherIncident : <<create>>
IncidentFactory ..> AirspaceIncident : <<create>>
Incident <|-- TechnicalIncident
Incident <|-- WeatherIncident
Incident <|-- AirspaceIncident
@enduml
```

Image: `UML_DIAGRAMS/03b-class-patterns.svg`

---

## 3c. Class Diagram, Part C: Login, Roles and Database

Roles and permissions, accounts, password hashing, sessions and the repositories that talk to SQLite.

```plantuml
@startuml
skinparam classAttributeIconSize 0
hide empty members
title Class Diagram (Part C): Login, Roles and Database — packages com.asims.auth, com.asims.repository

enum Permission {
  REPORT_INCIDENT
  ADD_EVIDENCE
  ESCALATE_INCIDENT
  DELETE_INCIDENT
  ASSIGN_INVESTIGATOR
  CLOSE_INCIDENT
}

enum Role {
  PILOT
  ADMIN
  INCIDENT_MANAGER
  - label : String
  - homePath : String
  + {abstract} canView(incident : Incident, user : Account) : boolean
  + has(permission : Permission) : boolean
  + getLabel() : String
  + getHomePath() : String
}

class Account {
  - username : String
  - fullName : String
  - role : Role
  - staffId : String
  - passwordHash : String
  + checkPassword(password : String) : boolean
  + can(permission : Permission) : boolean
}

class PasswordHasher {
  + {static} hash(password : String) : String
  + {static} matches(password : String, stored : String) : boolean
}

class AuthService {
  - accounts : AccountRepository
  - sessions : Map<String, Account>
  + login(username : String, password : String) : String
  + getAccount(token : String) : Account
  + logout(token : String) : void
}

class Database {
  - connection : Connection
  + {static} openFile(file : Path) : Database
  + {static} inMemory() : Database
  + getConnection() : Connection
}

class AccountRepository {
  + save(account : Account) : void
  + findByUsername(username : String) : Account
  + count() : int
}

class IncidentRepository {
  + save(incident : Incident) : void
  + delete(incidentId : String) : void
  + loadAll(fleet, pilots, investigators) : List<Incident>
  + countEvidence(incidentId : String) : int
}

class AircraftRepository {
  + save(aircraft : Aircraft) : void
  + loadAll() : List<Aircraft>
}

class StaffRepository {
  + savePilot(pilot : Pilot) : void
  + saveInvestigator(investigator : Investigator) : void
  + loadPilots() : List<Pilot>
  + loadInvestigators() : List<Investigator>
}

class SeedData {
  + {static} seedIfEmpty(aircraftRepo, staffRepo, accountRepo, incidentRepo) : void
}

class IncidentFactory <<Factory>>
class WebServer

Account "0..*" --> "1" Role
Role ..> Permission : grants >
Account ..> PasswordHasher : <<use>>
AuthService "1" --> "1" AccountRepository
AuthService "1" o-- "0..*" Account : sessions >
WebServer --> "1" AuthService
AccountRepository --> "1" Database
IncidentRepository --> "1" Database
AircraftRepository --> "1" Database
StaffRepository --> "1" Database
IncidentRepository ..> IncidentFactory : <<rebuilds subclasses>>
SeedData ..> AircraftRepository
SeedData ..> StaffRepository
SeedData ..> AccountRepository
SeedData ..> IncidentRepository
@enduml
```

Image: `UML_DIAGRAMS/03c-class-security.svg`

---

## 4. Object Diagram: system state after start-up

Run-time snapshot using the seeded data (incident INC-103 with its evidence, the pilot, the investigator, the observers and two logged-in accounts).

```plantuml
@startuml
object "service : IncidentService" as service
object "registry : IncidentRegistry" as registry
object "repo : IncidentRepository" as repo
object "db : Database" as db {
  file = "data/asims.db"
}
object "auth : AuthService" as auth
object "fleet : Fleet" as fleet {
  airlineName = "Pakistan International Airlines"
}
object "ap_bid : Aircraft" as ap_bid {
  registration = "AP-BID"
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
object "inc103 : TechnicalIncident" as inc103 {
  id = "INC-103"
  flightNumber = "PK-304"
  location = "Karachi (OPKC)"
  severity = 4
  status = UNDER_INVESTIGATION
  aircraftSystem = "Hydraulics"
  grounded = true
}
object "e1 : EvidenceLog" as e1 {
  id = "INC-103-E1"
  collectedBy = "Capt. Tariq Mahmood"
}
object "e2 : EvidenceLog" as e2 {
  id = "INC-103-E2"
  collectedBy = "Sara Ahmed"
}
object "tariq : Account" as tariq {
  username = "tariq"
  role = PILOT
  staffId = "P1"
}
object "admin : Account" as admin {
  username = "admin"
  role = ADMIN
}
object ":AdminNotifier" as an
object ":IncidentManagementNotifier" as mn

service --> registry
service --> repo
service --> fleet
service --> an
service --> mn
repo --> db
auth --> db : via AccountRepository
auth o-- tariq : session
auth o-- admin : session
fleet o-- ap_bid
registry o-- inc103
inc103 *-- e1
inc103 *-- e2
inc103 --> ap_bid : aircraft
inc103 --> p1 : reportedBy
inc103 --> i1 : assignedInvestigator
tariq ..> p1 : staffId
@enduml
```

Image: `UML_DIAGRAMS/04-object.svg`

---

## 5. Component Diagram

Components with provided interfaces (lollipops): REST/JSON API, incident operations, IncidentObserver and JDBC.

```plantuml
@startuml
component "Portal Pages\n(login, pilot, admin,\nmanager .html)" as UI
component "WebServer" as WEB
component "AuthService\n(login, sessions, roles)" as AUTH
component "IncidentService\n<<Facade>>" as FACADE
component "IncidentRegistry\n<<Singleton>>" as REG
component "IncidentFactory\n<<Factory>>" as FAC
component "Notifiers\n(Admin, IncidentManagement,\nAuthority, Maintenance)" as OBS
component "Repositories\n(Incident, Account,\nAircraft, Staff)" as REPO
component "Domain Model\n(Incident, StaffMember,\nFleet, Aircraft, EvidenceLog)" as MODEL
database "SQLite database\n(data/asims.db)" as DB

interface "REST / JSON API" as API
interface "Incident operations" as OPS
interface "IncidentObserver" as IOBS
interface "JDBC" as JDBC

WEB - API
UI ..> API : HTTP
FACADE - OPS
WEB ..> OPS
WEB ..> AUTH
OBS - IOBS
FACADE ..> IOBS : notify
FACADE ..> REG
FACADE ..> FAC
FACADE ..> REPO
AUTH ..> REPO
FAC ..> MODEL
REG ..> MODEL
REPO - JDBC
DB ..> JDBC
@enduml
```

Image: `UML_DIAGRAMS/05-component.svg`

---

## 6. Sequence Diagram: login, report and escalate an incident

Time-ordered messages for a pilot logging in and reporting a grounded technical incident, then the admin escalating it.

```plantuml
@startuml
actor Pilot
actor Admin
participant ":WebServer" as WEB
participant ":AuthService" as AUTH
participant ":IncidentService" as SVC
participant ":IncidentRegistry" as REG
participant "IncidentFactory" as FAC
participant "inc : TechnicalIncident" as INC
participant ":IncidentRepository" as REPO
participant ":AdminNotifier" as ADM
participant ":IncidentManagementNotifier" as MGT

== Pilot logs in ==
Pilot -> WEB : POST /api/login (username, password)
activate WEB
WEB -> AUTH : login(username, password)
activate AUTH
AUTH --> WEB : session token
deactivate AUTH
WEB --> Pilot : cookie + redirect to /pilot
deactivate WEB

== Pilot reports a grounded technical incident ==
Pilot -> WEB : POST /api/report
activate WEB
WEB -> AUTH : getAccount(token)
AUTH --> WEB : pilotAccount
WEB -> SVC : reportIncident(pilotAccount, "technical", ...)
activate SVC
SVC -> SVC : requirePermission(REPORT_INCIDENT)
SVC -> REG : generateId()
REG --> SVC : "INC-110"
SVC -> FAC : create("technical", ...)
activate FAC
create INC
FAC -> INC : new TechnicalIncident(...)
FAC --> SVC : inc
deactivate FAC
SVC -> REG : add(inc)
SVC -> REPO : save(inc)
SVC -> ADM : update("REPORTED", inc)
activate ADM
ADM -> INC : calculateRiskScore()
INC --> ADM : 88
deactivate ADM
SVC --> WEB : inc
deactivate SVC
WEB --> Pilot : {"ok": true}
deactivate WEB

== Admin escalates it ==
Admin -> WEB : POST /api/escalate
activate WEB
WEB -> SVC : escalateIncident(adminAccount, "INC-110")
activate SVC
SVC -> SVC : requirePermission(ESCALATE_INCIDENT)
SVC -> REG : findById("INC-110")
REG --> SVC : inc
SVC -> INC : escalate()
SVC -> REPO : save(inc)
SVC -> MGT : update("ESCALATED", inc)
SVC --> WEB : (void)
deactivate SVC
WEB --> Admin : {"ok": true}
deactivate WEB
@enduml
```

Image: `UML_DIAGRAMS/06-sequence.svg`

---

## 7. Communication Diagram: login, report and escalate

The same scenario as the sequence diagram, drawn as numbered messages on object links.

```plantuml
@startuml
left to right direction
object "pilot : Browser" as P
object "admin : Browser" as A
object ":WebServer" as WEB
object ":AuthService" as AUTH
object ":IncidentService" as SVC
object ":IncidentRegistry" as REG
object "IncidentFactory" as FAC
object "inc : TechnicalIncident" as INC
object ":IncidentRepository" as REPO
object ":AdminNotifier" as ADM
object ":IncidentManagementNotifier" as MGT

P --> WEB : 1: POST /api/login\n2: POST /api/report
A --> WEB : 3: POST /api/escalate
WEB --> AUTH : 1.1: login(username, password)\n2.1: getAccount(token)\n3.1: getAccount(token)
WEB --> SVC : 2.2: reportIncident(account, ...)\n3.2: escalateIncident(account, id)
SVC --> REG : 2.2.1: generateId()\n2.2.3: add(inc)\n3.2.1: findById(id)
SVC --> FAC : 2.2.2: create(type, ...)
FAC --> INC : 2.2.2.1: <<create>>
SVC --> REPO : 2.2.4: save(inc)\n3.2.3: save(inc)
SVC --> ADM : 2.2.5: update("REPORTED", inc)
SVC --> INC : 3.2.2: escalate()
SVC --> MGT : 3.2.4: update("ESCALATED", inc)
ADM --> INC : 2.2.5.1: calculateRiskScore()
@enduml
```

Image: `UML_DIAGRAMS/07-communication.svg`

---

## 8. Deployment Diagram

Client workstation with a web browser, and the application server (JVM) that holds the SQLite database file.

```plantuml
@startuml
node "Client Workstation" as client {
  node "Web Browser" {
    artifact "login.html, pilot.html,\nadmin.html, manager.html\n(HTML/CSS/JS)" as html
  }
}

node "Application Server" as server {
  node "Java Runtime (JVM 17+)" {
    artifact "asims-1.0 classes\n(com.asims.*)" as app
    artifact "gson-2.11.0.jar" as gson
    artifact "sqlite-jdbc-3.47.jar" as jdbc
  }
  node "File System" {
    database "SQLite database file\ndata/asims.db" as db
  }
}

client -- server : HTTP / REST (JSON)\nsession cookie, localhost:8000
app ..> gson
app ..> jdbc
jdbc ..> db : JDBC
@enduml
```

Image: `UML_DIAGRAMS/08-deployment.svg`

---
