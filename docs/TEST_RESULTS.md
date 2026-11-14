# ASIMS — Test Results

- **Framework:** JUnit 5.11.4
- **Test file:** `src/test/java/com/asims/AsimsTest.java`
- **Command:** `mvnw.cmd test`
- **Run date:** 14 Nov 2026
- **Environment:** OpenJDK 25.0.3, compiled for Java 17

## Summary
| Tests run | Passed | Failed | Errors | Skipped |
|---|---|---|---|---|
| 11 | 11 | 0 | 0 | 0 |

## Test cases
| ID | Concept tested | Input / action | Expected result | Actual | Status |
|---|---|---|---|---|---|
| T1 | Singleton | Call `IncidentRegistry.getInstance()` twice | Both calls return the same object | Same object | ✅ Pass |
| T2 | Factory | `IncidentFactory.create()` with "technical", "weather", "airspace", "volcano" | Correct subclass for each type; "volcano" throws `IllegalArgumentException` | As expected | ✅ Pass |
| T3 | Polymorphism | Three incident types, all with severity 3, calling `calculateRiskScore()` | Technical 76, Weather 50, Airspace 80 | 76 / 50 / 80 | ✅ Pass |
| T4 | Inheritance / Liskov Substitution | Loop over `List<Incident>` and `List<StaffMember>` | Every subclass works through the parent type | Works | ✅ Pass |
| T5 | Encapsulation | `setSeverity(9)` and `setSeverity(0)` | `IllegalArgumentException`; severity unchanged | Rejected, still 4 | ✅ Pass |
| T6 | Encapsulation | `getEvidenceLogs().clear()` | `UnsupportedOperationException` (the list is read-only) | Thrown | ✅ Pass |
| T7 | Observer | Report an incident, then escalate it | `AuthorityNotifier` receives REPORTED, then ESCALATED | 2 notifications | ✅ Pass |
| T8 | Observer | Report one non-grounded and one grounded technical incident | `MaintenanceNotifier` is notified only for the grounded one | 0, then 1 | ✅ Pass |
| T9 | Composition | Add evidence, then delete the incident | The evidence is gone together with the incident | Gone | ✅ Pass |
| T10 | Aggregation | Remove the aircraft from the `Fleet` | The aircraft still exists and is still referenced by the incident | Still exists | ✅ Pass |
| T11 | Workflow | Close before assigning, then assign → escalate → close → escalate again | Status goes REPORTED → UNDER_INVESTIGATION → ESCALATED → CLOSED; both illegal steps throw | As expected | ✅ Pass |

## Console output
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.asims.AsimsTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.202 s -- in com.asims.AsimsTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] BUILD SUCCESS
```

## Manual system test (web API)
These checks were run against the live server on `localhost:8000`.

| Action | Result |
|---|---|
| `GET /` | 200, the dashboard loads |
| Report an airspace incident (severity 5, separation 0.6 nm) | Created INC-104 with risk 100 CRITICAL; the authority was notified |
| Report with severity 9 | Rejected: "Severity must be between 1 and 5, got 9" |
| Escalate INC-103 | Status ESCALATED; the authority received an ESCALATED message |
| Close INC-103 with no investigator assigned | Rejected: "An investigator must be assigned before closing." |
| Add evidence, then delete INC-102 | The incident and its evidence were removed |
| Grounded hydraulic failure (INC-101) | The Maintenance Team received "Inspect aircraft AP-BXK..." |
