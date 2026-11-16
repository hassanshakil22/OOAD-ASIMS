# ASIMS — Test Results

- **Framework:** JUnit 5.11.4, using a temporary in-memory SQLite database
- **Test file:** `src/test/java/com/asims/AsimsTest.java`
- **Command:** `mvnw.cmd test`
- **Run date:** 16 Nov 2026
- **Environment:** OpenJDK 25.0.3, compiled for Java 17

## Summary
| Tests run | Passed | Failed | Errors | Skipped |
|---|---|---|---|---|
| 17 | 17 | 0 | 0 | 0 |

## Test cases
| ID | Concept tested | Action | Expected result | Status |
|---|---|---|---|---|
| T1 | Singleton | Call `IncidentRegistry.getInstance()` twice | Same object both times | ✅ Pass |
| T2 | Factory | `IncidentFactory.create()` with "technical", "weather", "airspace", "volcano" | Correct subclass each time; "volcano" is rejected | ✅ Pass |
| T3 | Polymorphism | Three types with severity 3 | Risk 76 (technical), 50 (weather), 80 (airspace) | ✅ Pass |
| T4 | Inheritance / Liskov | Loop over `List<Incident>` and `List<StaffMember>` | Every subclass works through the parent type | ✅ Pass |
| T5 | Encapsulation | `setSeverity(9)` and `setSeverity(0)` | Rejected, value unchanged | ✅ Pass |
| T6 | Encapsulation | `getEvidenceLogs().clear()` | `UnsupportedOperationException` | ✅ Pass |
| T7 | Observer | Report an incident, then escalate it | Authority receives REPORTED, then ESCALATED | ✅ Pass |
| T8 | Observer | Report a non-grounded and a grounded incident | Maintenance notified only for the grounded one | ✅ Pass |
| T9 | Observer | Report, then escalate | Admin inbox gets the report; incident management gets only the escalation | ✅ Pass |
| T10 | Composition | Add evidence, then delete the incident | Evidence gone in memory and in the database (`ON DELETE CASCADE`) | ✅ Pass |
| T11 | Aggregation | Remove the aircraft from the `Fleet` | Aircraft still exists and is still referenced | ✅ Pass |
| T12 | Workflow | Report → escalate → assign → close across the three roles | Correct statuses; skipping or repeating a step is rejected | ✅ Pass |
| T13 | Roles | Every role tries the other roles' actions | `SecurityException`; nothing changes | ✅ Pass |
| T14 | Visibility | Two pilots report; admin escalates one | Pilots see only their own; manager sees only escalated; a pilot cannot add evidence to another pilot's incident | ✅ Pass |
| T15 | Login | Correct, wrong and unknown credentials; logout | Only the correct login works; stored value is a salted hash, never the password | ✅ Pass |
| T16 | Database | Save an incident, "restart", load it again | Same subclass, status, investigator, evidence and risk score; new ids continue | ✅ Pass |
| T17 | Seed data | Seed an empty database twice | 8 aircraft, 4 pilots, 6 accounts; demo logins work; second run adds nothing | ✅ Pass |

## Console output
```
[INFO] Running com.asims.AsimsTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.011 s -- in com.asims.AsimsTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Manual system test (live server)
| Action | Result |
|---|---|
| Log in with a wrong password | Rejected: "Invalid username or password." |
| Open `/admin` without logging in / as a pilot | Redirected to `/login` / to the pilot's own portal |
| Pilot reports a grounded technical incident | Saved as INC-110 (88 CRITICAL); in the admin inbox; maintenance notified |
| Pilot tries to escalate | Refused (403): "Pilot accounts are not allowed to do this." |
| Manager assigns before escalation | Refused: only an escalated incident can be assigned |
| Admin escalates, then tries to close | Escalation works; close refused (403) |
| Manager assigns, adds evidence, closes | UNDER_INVESTIGATION, then CLOSED; the pilot sees CLOSED |
| Missing flight number / severity 9 / malformed request | Rejected with a clear message |
| Stop and restart the server | All incidents, evidence and statuses are still there; the next id is INC-111 |
