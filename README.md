# Smart Meeting Scheduler API

The Smart Meeting Scheduler API is a Spring Boot application designed to schedule conflict-free meetings for multiple users and retrieve their calendars. It uses an in-memory H2 database for data persistence and asynchronous processing for efficient scheduling.

## Table of Contents
- [Overview](#overview)
- [Design and Architecture](#design-and-architecture)
- [Data Models](#data-models)
- [Scheduling Algorithm and Heuristics](#scheduling-algorithm-and-heuristics)
- [Setup Instructions](#setup-instructions)
- [Testing the API](#testing-the-api)
- [Running Tests](#running-tests)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Overview
The API provides two REST endpoints:
- **`POST /api/schedule`**: Schedules a meeting for multiple users within a specified time range, avoiding conflicts with existing events.
- **`GET /api/users/{userId}/calendar`**: Retrieves a user’s events within a given time range.

**Tech Stack**:
- **Backend**: Spring Boot 3.3.2, Java 21, Spring Data JPA, Hibernate 6.5.2
- **Database**: In-memory H2 Database 2.2.224
- **Async Processing**: `@Async` with `ThreadPoolTaskExecutor`
- **Testing**: JUnit 5, Postman
- **Build Tool**: Maven
- **Version Control**: Git, GitHub

The source code is hosted at: **https://github.com/Ritikesh21/smart-meeting-scheduler**

## Design and Architecture

### Architecture
The application follows a layered architecture for modularity and scalability:
- **Controller Layer** (`EventController`): Manages HTTP requests/responses using `@RestController` and validates inputs with `@Valid`.
- **Service Layer** (`EventService`): Handles business logic for scheduling and calendar retrieval, leveraging `@Async` for performance.
- **Repository Layer** (`UserRepository`, `EventRepository`): Extends `JpaRepository` for efficient database operations.
- **Entity Layer** (`User`, `Event`): Defines JPA entities for data persistence.
- **Configuration**:
  - `AsyncConfig`: Configures a `ThreadPoolTaskExecutor` for asynchronous tasks.
  - `application.properties`: Sets up H2 with a `create-drop` strategy.

### Design Choices
- **Spring Boot**: Simplifies development with auto-configuration and an embedded Tomcat server.
- **H2 Database**: Lightweight and ideal for development/testing, requiring no external setup.
- **JPA/Hibernate**: Minimizes boilerplate code with ORM and repository abstractions.
- **Asynchronous Processing**: Enhances performance for scheduling computations.
- **RESTful Design**: Follows REST principles for intuitive and standardized endpoints.
- **Error Handling**: Returns clear HTTP status codes (`400`, `404`, `409`) for invalid inputs, missing users, or scheduling conflicts.

## Data Models

1. **User Entity**:
   - **Table**: `users` (renamed to avoid H2 reserved keyword `user`)
   - **Fields**:
     - `id` (String, primary key): Unique user identifier (e.g., `user1`)
     - `name` (String): User’s name (e.g., `Alice`)
   - **Annotations**: `@Entity`, `@Table(name = "users")`, `@Id`
   - **Purpose**: Represents users who can participate in meetings.

2. **Event Entity**:
   - **Table**: `event`
   - **Fields**:
     - `id` (Long, auto-generated): Unique event identifier
     - `title` (String): Event name (e.g., `Team Sync`)
     - `startTime` (Instant): Event start time in UTC
     - `endTime` (Instant): Event end time in UTC
     - `user` (Many-to-One): Linked to `User` via `user_id` foreign key
   - **Annotations**: `@Entity`, `@GeneratedValue`, `@ManyToOne`, `@JoinColumn(name = "user_id")`
   - **Purpose**: Represents scheduled meetings or events.

## Scheduling Algorithm and Heuristics

### Algorithm
The `scheduleMeeting` method in `EventService` finds a conflict-free time slot for a meeting:
1. **Input**:
   - `participantIds`: List of user IDs (e.g., `["user1", "user2", "user3"]`)
   - `durationMinutes`: Meeting duration (e.g., 60 minutes)
   - `timeRange`: Start and end times (e.g., `2024-09-01T09:00:00Z` to `2024-09-05T17:00:00Z`)
2. **Validation**:
   - Verifies user existence using `UserRepository`.
   - Validates time range and duration with `@Valid`.
3. **Time Slot Search**:
   - Iterates through the time range in 30-minute increments.
   - For each slot (start to start + duration):
     - Queries `EventRepository` for each user’s events within the slot.
     - Ensures no overlap using a `for` loop (replaced `stream().allMatch` for clarity).
     - No overlap if: `slotEnd < eventStart` or `slotStart > eventEnd`.
4. **Event Creation**:
   - If a free slot is found, creates an `Event` for each participant.
   - Saves events via `EventRepository`.
   - Returns a response with meeting details (ID, title, participants, times).
5. **Error Handling**:
   - `400 Bad Request`: Invalid input (e.g., malformed time format).
   - `404 Not Found`: Non-existent user.
   - `409 Conflict`: No available time slot.

### Heuristics
- **30-Minute Increments**: Provides sufficient granularity while minimizing computational overhead.
- **Earliest Slot Priority**: Selects the earliest available slot to optimize scheduling.
- **Asynchronous Processing**: Uses `@Async` to parallelize conflict checks, improving performance.
- **Optimized Queries**: Queries only events within the relevant time range to reduce database load.

### Example
For a 60-minute meeting for `user1`, `user2`, `user3` from `2024-09-01T09:00:00Z` to `2024-09-05T17:00:00Z`:
- Skips `09:00-10:00` due to `user1`’s `Team Sync` event.
- Selects `11:00-12:00` if free for all users.
- Creates events and returns a `201 Created` response with meeting details.

## Setup Instructions

### Prerequisites
- **Java 21**: Install OpenJDK or Oracle JDK.
- **Maven 3.8+**: For dependency management and building.
- **Git**: For cloning the repository.
- **Postman** (optional): For API testing.
- **IDE** (optional): IntelliJ IDEA or VS Code for development.

### Steps
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Ritikesh21/smart-meeting-scheduler.git
   cd smart-meeting-scheduler
   ```

2. **Build the Project**:
   ```bash
   mvn clean install
   ```
   - Downloads dependencies and compiles the code.

3. **Configure the Application**:
   - The `application.properties` file is pre-configured for H2:
     ```properties
     spring.datasource.url=jdbc:h2:mem:testdb
     spring.datasource.driverClassName=org.h2.Driver
     spring.datasource.username=sa
     spring.datasource.password=password
     spring.h2.console.enabled=true
     spring.jpa.defer-datasource-initialization=true
     spring.jpa.hibernate.ddl-auto=create-drop
     ```

4. **Run the Application**:
   ```bash
   mvn spring-boot:run
   ```
   - Starts the application on `http://localhost:8080`.
   - Seeds initial data: users (`user1`, `user2`, `user3`) and events (e.g., `Team Sync`, `Lunch Break`).

5. **Verify Database** (Optional):
   - Access the H2 console at `http://localhost:8080/h2-console`.
   - Use JDBC URL: `jdbc:h2:mem:testdb`, Username: `sa`, Password: `password`.
   - Run queries to inspect data:
     ```sql
     SELECT * FROM users;
     SELECT * FROM event;
     ```

## Testing the API

### Using Postman or curl
1. **POST /api/schedule**:
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
   - **Expected Response**: `201 Created` with meeting details (e.g., `startTime: "2024-09-01T11:00:00Z"`).

2. **GET /api/users/user1/calendar**:
   ```bash
   curl "http://localhost:8080/api/users/user1/calendar?start=2024-09-01T00:00:00Z&end=2024-09-05T23:59:59Z"
   ```
   - **Expected Response**: `200 OK` with `user1`’s events (e.g., `Team Sync`, `Lunch Break`).

3. **Postman Collection** (Optional):
   - Import `SmartMeetingSchedulerAPI.postman_collection.json` (available in the repository or provided separately) into Postman.
   - Set the `baseUrl` variable to `http://localhost:8080`.
   - Run requests to test success and error cases (e.g., invalid time, non-existent user).

## Running Tests

### Unit Tests
The project includes JUnit 5 tests in `src/test/java` (`EventServiceTest`).
1. **Run Tests**:
   ```bash
   mvn test
   ```
   - Tests validate:
     - Scheduling with valid/invalid inputs.
     - Calendar retrieval for users.
     - Edge cases (e.g., conflicts, non-existent users).

2. **Verify Results**:
   - Check the console for test output.
   - Ensure all tests pass, confirming `EventService` logic and async handling with `CompletableFuture`.

## Troubleshooting
- **H2 Database Error (`user` table)**:
  - Issue: H2 reserved keyword caused `JdbcSQLSyntaxErrorException`.
  - Fix: Added `@Table(name = "users")` to `User.java`.
  - Verify: Check schema in H2 console (`http://localhost:8080/h2-console`).
- **Async Scheduling Bug**:
  - Issue: `stream().allMatch` in `EventService` caused incorrect conflict checks.
  - Fix: Replaced with a `for` loop for clarity and correctness.
- **Git Push Errors**:
  - `src refspec main does not match any`: Renamed branch (`git branch -m master main`).
  - `failed to push some refs`: Pulled remote changes (`git pull origin main --rebase`) and resolved conflicts.
- **Test Failures**:
  - Ensured `CompletableFuture.get()` in `EventServiceTest` for async testing.
- **Logs**:
  - Enable debug logging in `application.properties`:
    ```properties
    logging.level.org.springframework.web=DEBUG
    logging.level.com.example.smartmeetingscheduler=DEBUG
    ```

## Contributing
1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/your-feature`.
3. Commit changes: `git commit -m "Add your feature"`.
4. Push to the branch: `git push origin feature/your-feature`.
5. Open a Pull Request on GitHub.

## License
[MIT License](LICENSE) (optional, create a `LICENSE` file if needed).
