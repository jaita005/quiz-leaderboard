# Quiz Leaderboard System

**SRM Internship Assignment — Bajaj Finserv Health | JAVA Qualifier**

---

## Problem Overview

This application consumes a quiz validator API across **10 sequential polls**, deduplicates events at the `(roundId + participant)` level, aggregates per-participant scores, and submits a sorted leaderboard exactly once.

---

## Architecture

```
Main.java                   ← Orchestrator: poll loop + submit
│
├── QuizApiClient.java       ← HTTP layer  (GET /quiz/messages, POST /quiz/submit)
│
├── LeaderboardService.java  ← Business logic (dedup + aggregation + sort)
│
└── model/
    ├── QuizEvent.java       ← One score event (roundId, participant, score)
    ├── PollResponse.java    ← GET response wrapper
    ├── LeaderboardEntry.java← One leaderboard row
    ├── SubmitRequest.java   ← POST request body
    └── SubmitResponse.java  ← POST response body
```

---

## Key Design Decisions

### 1. Deduplication Strategy

The assignment states that **the same API response data may appear again in later polls**. The fix is a `HashSet`-like `seen` map keyed on:

```
deduplicationKey = roundId + "::" + participant
```

Only the **first** occurrence of each key is counted; subsequent appearances are silently discarded.

```
Poll 0 → { roundId: "R1", participant: "Alice", score: 10 }  → ACCEPTED → Alice = 10
Poll 3 → { roundId: "R1", participant: "Alice", score: 10 }  → DUPLICATE → ignored
                                                                Alice stays = 10  ✓
```

### 2. Mandatory 5-Second Inter-Poll Delay

Per the specification, a **5-second delay** is maintained between every consecutive poll:

```java
Thread.sleep(5_000L);
```

Total minimum runtime: 9 × 5 s = **45 seconds**.

### 3. Submit Exactly Once

The leaderboard is submitted only after **all 10 polls** have been successfully processed, ensuring the data is complete and consistent.

### 4. Leaderboard Sort Order

Sorted **descending by totalScore** (highest scorer appears first), matching standard leaderboard conventions.

---

## Build

```bash
git clone https://github.com/jaita005/quiz-leaderboard.git
cd quiz-leaderboard
mvn clean package -q
```

This produces a fat JAR at:
```
target/quiz-leaderboard.jar
```

---

## Run

```bash
java -jar target/quiz-leaderboard.jar <YOUR_REG_NO>
```

## Console Output

```
19:01:36.694 [main] INFO com.quiz.Main - Starting Quiz Leaderboard System | regNo=RA2311032020057 
19:01:37.286 [main] INFO com.quiz.Main - Fetching poll 0/9 
19:01:37.292 [main] INFO com.quiz.client.QuizApiClient - ? GET https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/messages?regNo=RA2311032020057&poll=0 (poll=0) 
19:01:37.775 [main] INFO com.quiz.client.QuizApiClient - ? HTTP 200 for poll=0 | body: {"regNo":"RA2311032020057","pollIndex":0,"totalPolls":45,"events":[{"roundId":"R1","participant":"Alice","score":120},{"roundId":"R1","participant":"Bob","score":95}],"meta":{"hint":"Rounds and scores may repeat across polls. Handle them correctly.","totalPollsRequired":10}} 
19:01:37.870 [main] INFO com.quiz.Main - Poll 0 complete. Events in payload: 2
...
(8 more poll cycles)
...
=======================================================
LEADERBOARD  (events accepted=9, duplicates rejected=6)
=======================================================
Participant          Total Score
-------------------------------------------------------
Bob                          295
Alice                        280
Charlie                      260
-------------------------------------------------------
GRAND TOTAL                  835
=======================================================

══════════════════════════════════════════════════════
SUBMISSION RESULT
══════════════════════════════════════════════════════
  isCorrect      : false
  isIdempotent   : false
  submittedTotal : 835
  expectedTotal  : 0
  message        : null
══════════════════════════════════════════════════════
```
## Sample API Response

<img width="2560" height="1366" alt="image" src="https://github.com/user-attachments/assets/39eed939-182a-4eff-b057-66f84244197d" />


---

## Flow Summary

```
┌─────────────────────────────────────────────────────┐
│                  Main (Orchestrator)                 │
│                                                     │
│  for poll = 0 → 9:                                  │
│    ┌──────────────────────────────────────────────┐ │
│    │  GET /quiz/messages?regNo=X&poll=N           │ │
│    │         ↓                                    │ │
│    │  LeaderboardService.processEvents(events)    │ │
│    │    ├─ check seenEvents map (dedup key)       │ │
│    │    ├─ if NEW  → add to scores map            │ │
│    │    └─ if SEEN → discard silently             │ │
│    └──────────────────────────────────────────────┘ │
│    sleep(5 seconds)                                 │
│                                                     │
│  buildLeaderboard() → sort descending               │
│  computeGrandTotal()                                │
│  POST /quiz/submit  (exactly once)                  │
└─────────────────────────────────────────────────────┘
```

---

## Project Structure

```
quiz-leaderboard/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/com/quiz/
        │   ├── Main.java
        │   ├── client/
        │   │   └── QuizApiClient.java
        │   ├── service/
        │   │   └── LeaderboardService.java
        │   └── model/
        │       ├── QuizEvent.java
        │       ├── PollResponse.java
        │       ├── LeaderboardEntry.java
        │       ├── SubmitRequest.java
        │       └── SubmitResponse.java
        └── resources/
            └── logback.xml
```

---

## Dependencies

| Library          | Purpose                        |
|------------------|--------------------------------|
| Jackson Databind | JSON serialization/parsing     |
| SLF4J + Logback  | Structured logging             |
| java.net.http    | HTTP client (built-in, JDK 11+)|

---

## Troubleshooting

- **HTTP 4xx error** → Check if `regNo` is valid
- **isCorrect: false** → Issue in deduplication logic (roundId + participant handling)
- **Program stops early** → Don’t interrupt during polling (~50s runtime)
- **Build issues** → Use Java 17+ and Maven 3.8+
---

## Author

**Registration No:** `RA2311032020057`  
**Assignment:** Bajaj Finserv Health | JAVA Qualifier | SRM | Apr 2025
