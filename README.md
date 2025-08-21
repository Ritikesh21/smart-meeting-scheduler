# Smart Meeting Scheduler API

## Overview
The Smart Meeting Scheduler is a RESTful API built with Java, Spring Boot 3.3.2, and H2 in-memory database. It schedules meetings for multiple users by finding an optimal time slot based on their existing calendar events and a scoring algorithm. Advanced Spring features include asynchronous processing, validation, custom exception handling, and optimized JPA queries.

## Design and Architectural Choices
- **Framework**: Spring Boot for dependency injection, RESTful APIs, and rapid development.
- **Database**: H2 in-memory for simplicity and easy setup.
- **Data Models**:
  - **User**: `id` (String, PK), `name` (String).
  - **Event**: `id` (Long, auto-generated), `title` (String), `startTime` (Instant), `endTime` (Instant), `user` (Many-to-One).
- **Advanced Features**:
  - **Asynchronous Processing**: `@EnableAsync` and `ThreadPoolTaskExecutor` for non-blocking API calls.
  - **Validation**: Jakarta Bean Validation for robust input validation.
  - **Exception Handling**: `@ControllerAdvice` for centralized error management.
  - **JPA Optimization**: Custom queries for efficient overlap detection and gap analysis.
  - **Transactions**: `@Transactional` for atomicity during booking.
- **API Design**: RESTful with `/api` prefix, clear endpoints, and proper HTTP status codes.

## Algorithm for Finding Optimal Meeting Time
1. **Input Validation**: Validate `participantIds`, `durationMinutes`, and `timeRange` using Jakarta Bean Validation and custom checks.
2. **Slot Enumeration**: Iterate over possible start times in 15-minute steps within the time range.
3. **Availability Check**: Use `EventRepository.findOverlapping` to ensure no conflicts for all participants.
4. **Scoring**:
   - **Earlier Slots**: `100000 - minutes_from_range_start`.
   - **Working Hours**: +500 if within 9:00-17:00 UTC.
   - **Minimize Gaps**:
     - +100 for back-to-back (gap = 0 min).
     - -50 for awkward gaps (<30 min).
     - +50 for large gaps (≥60 min).
   - **Buffer Time**: +25 per participant for ≥15-min gaps before/after.
   - Select the highest-scoring slot, breaking ties by earliest start.
5. **Booking**: Create `Event` entries transactionally for all participants.
6. **Error Handling**:
   - 400 Bad Request: Invalid users, time format, or range.
   - 409 Conflict: No available slot.
   - 500 Internal Server Error: Unexpected issues.

## Setup and Running
### Prerequisites
- Java 21
- Maven 3.8+
- Git
- IDE (IntelliJ IDEA, Eclipse, or VS Code with Java extensions)

### Steps
1. **Generate Project**:
   - Visit [start.spring.io](https://start.spring.io).
   - Configure:
     - Project: Maven, Language: Java, Spring Boot: 3.3.2
     - Group: com.example, Artifact: smart-meeting-scheduler
     - Dependencies: Spring Web, Spring Data JPA, H2 Database, Lombok, Spring Boot Starter Validation, Spring Boot Starter Test
     - Java: 21
   - Download and unzip the project.
2. **Set Up Directory Structure**:
   - Create subdirectories in `src/main/java/com/example/smartmeetingscheduler/`: `config`, `entities`, `repositories`, `dto`, `services`, `controllers`, `exceptions`.
   - Create `src/main/resources/application.properties`.
   - Create `src/test/java/com/example/smartmeetingscheduler/` for tests.
   - Add `README.md` in the root.
3. **Add Files**:
   - Copy the provided `pom.xml`, Java classes, `application.properties`, and `README.md` into the respective directories.
4. **Build**:
   ```bash
   cd smart-meeting-scheduler
   mvn clean install
   ```
5. **Run**:
   ```bash
   mvn spring-boot:run
   ```
   - API: `http://localhost:8080/api`
   - H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:testdb`, Username: `sa`, Password: `password`)

### Initial Data
- Users: `user1` (Alice), `user2` (Bob), `user3` (Charlie).
- Events:
  - Alice: "Team Sync" (2024-09-01 09:00-10:00Z), "Lunch Break" (2024-09-01 12:00-13:00Z), "Daily Standup" (2024-09-04 09:30-10:00Z).
  - Bob: "Project Review" (2024-09-02 14:00-15:00Z).
  - Charlie: "Client Call" (2024-09-03 10:00-11:00Z).

## Testing
### Unit/Integration Tests
```bash
mvn test
```
- Tests `EventService` for successful scheduling, no-slot scenarios, and calendar retrieval.
- Expand with more tests for edge cases in production.

### API Testing
#### POST /api/schedule
```bash
curl -X POST http://localhost:8080/api/schedule \
-H "Content-Type: application/json" \
-d '{
    "participantIds": ["user1", "user2", "user3"],
    "durationMinutes": 60,
    "timeRange": {
        "start": "2024-09-01T09:00:00Z",
        "end": "2024-09-05T17:00:00Z"
    }
}'
```
**Response** (201):
```json
{
    "meetingId": "meeting-uuid",
    "title": "New Meeting",
    "participantIds": ["user1", "user2", "user3"],
    "startTime": "2024-09-01T11:00:00Z",
    "endTime": "2024-09-01T12:00:00Z"
}
```

#### GET /api/users/{userId}/calendar
```bash
curl "http://localhost:8080/api/users/user1/calendar?start=2024-09-01T00:00:00Z&end=2024-09-05T23:59:59Z"
```
**Response** (200):
```json
[
    {
        "title": "Team Sync",
        "startTime": "2024-09-01T09:00:00Z",
        "endTime": "2024-09-01T10:00:00Z"
    },
    {
        "title": "Lunch Break",
        "startTime": "2024-09-01T12:00:00Z",
        "endTime": "2024-09-01T13:00:00Z"
    },
    {
        "title": "Daily Standup",
        "startTime": "2024-09-04T09:30:00Z",
        "endTime": "2024-09-04T10:00:00Z"
    }
]
```

### Postman Collection
1. Open Postman and create a new collection named "Smart Meeting Scheduler".
2. Add a POST request:
   - URL: `http://localhost:8080/api/schedule`
   - Body: Raw JSON (use the above POST example).
3. Add a GET request:
   - URL: `http://localhost:8080/api/users/{{userId}}/calendar`
   - Params: `start=2024-09-01T00:00:00Z`, `end=2024-09-05T23:59:59Z`
   - Set `userId` as a variable (e.g., `user1`).
4. Save and test the collection.

## Advanced Spring Features
- **Asynchronous Processing**: `@Async` with `CompletableFuture` for non-blocking operations, configured via `AsyncConfig`.
- **Validation**: Jakarta Bean Validation with custom error messages.
- **Exception Handling**: Centralized with `@ControllerAdvice` for consistent error responses.
- **JPA Optimization**: Custom queries (`findOverlapping`, `findMaxEndBefore`, `findMinStartAfter`) for efficient data access.
- **Transactional Management**: `@Transactional` ensures atomicity during booking.
- **Testing**: Integration tests with `@SpringBootTest` for core functionality.
