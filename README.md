# Smart Meeting Scheduler API

The Smart Meeting Scheduler API is a Spring Boot application designed to schedule conflict-free meetings for multiple users and retrieve user calendars. It uses an in-memory H2 database for persistence and supports asynchronous processing for efficient scheduling.

## Design and Architectural Choices

### Architecture
The application follows a layered architecture typical of Spring Boot applications, ensuring modularity, maintainability, and scalability:

- **Controller Layer** (`EventController`):
  - Handles HTTP requests and responses.
  - Exposes two endpoints:
    - `POST /api/schedule`: Schedules a meeting for multiple users.
    - `GET /api/users/{userId}/calendar`: Retrieves a user’s events within a time range.
  - Uses `@RestController` and `@Valid` for request validation.
- **Service Layer** (`EventService`):
  - Contains business logic for scheduling and calendar retrieval.
  - Uses `@Async` to perform time slot calculations concurrently, improving performance.
  - Validates inputs and checks for conflicts using repository queries.
- **Repository Layer** (`UserRepository`, `EventRepository`):
  - Interfaces extending `JpaRepository` for CRUD operations on `User` and `Event` entities.
  - Automatically generates queries for fetching events by user and time range.
- **Entity Layer** (`User`, `Event`):
  - Defines data models mapped to database tables using JPA annotations.
  - Configured to avoid H2 reserved keywords (e.g., `@Table(name = "users")`).
- **Configuration**:
  - `AsyncConfig`: Defines a `ThreadPoolTaskExecutor` bean for async processing.
  - `application.properties`: Configures H2 database (`jdbc:h2:mem:testdb`) with `create-drop` strategy for development.

### Data Models
The application uses two JPA entities:

1. **User**:
   - **Table**: `users` (renamed from `user` to avoid H2 reserved keyword).
   - **Fields**:
     - `id` (String, primary key): Unique identifier for a user (e.g., `user1`).
     - `name` (String): User’s name (e.g., `Alice`).
   - **Annotations**:
     - `@Entity`, `@Table(name = "users")`, `@Id`.
   - **Purpose**: Represents a user who can participate in meetings.

2. **Event**:
   - **Table**: `event`.
   - **Fields**:
     - `id` (Long, auto-generated): Unique event identifier.
     - `title` (String): Event name (e.g., `Team Sync`).
     - `startTime` (Instant): Event start time (UTC).
     - `endTime` (Instant): Event end time (UTC).
     - `user` (Many-to-One with `User`): Associated user, linked via `user_id` foreign key.
   - **Annotations**:
     - `@Entity`, `@GeneratedValue`, `@ManyToOne`, `@JoinColumn(name = "user_id")`.
   - **Purpose**: Represents a scheduled meeting or event.

### Design Choices
- **Spring Boot**: Chosen for its auto-configuration, embedded Tomcat server, and dependency management, reducing setup time.
- **H2 Database**: Used for its lightweight, in-memory nature, ideal for development and testing.
- **JPA/Hibernate**: Simplifies database operations with ORM, minimizing manual SQL.
- **Async Processing**: `@Async` ensures scheduling computations don’t block the main thread, improving scalability.
- **RESTful Design**: Follows REST principles with clear endpoint semantics (`POST` for creation, `GET` for retrieval).
- **Error Handling**: Custom exceptions and HTTP status codes (e.g., `400` for invalid input, `409` for conflicts) enhance robustness.

## Scheduling Algorithm and Heuristics

### Algorithm Overview
The `scheduleMeeting` method in `EventService` finds a conflict-free time slot for a meeting involving multiple users. It operates as follows:

1. **Input Validation**:
   - Accepts `participantIds` (list of user IDs), `durationMinutes`, and `timeRange` (start/end `Instant`).
   - Validates inputs using `@Valid` and checks if users exist via `UserRepository`.

2. **Time Slot Iteration**:
   - Iterates through the time range in 30-minute increments (configurable heuristic).
   - For each slot (e.g., `2024-09-01T09:00:00Z` to `2024-09-01T10:00:00Z` for a 60-minute meeting):
     - Queries `EventRepository` to fetch events for each participant within the slot.
     - Checks if the slot is free (no overlapping events) for all participants.

3. **Conflict Check**:
   - For each user’s events, verifies that the proposed slot’s start and end times don’t overlap:
     - No overlap if: `slotEnd < eventStart` or `slotStart > eventEnd`.
   - Uses a `for` loop (replaced original `stream().allMatch` for clarity) to ensure all users are free.

4. **Event Creation**:
   - If a free slot is found, creates an `Event` for each participant with the same title and time.
   - Saves events via `EventRepository` and returns a response with meeting details.

5. **Error Handling**:
   - Returns `400 Bad Request` for invalid inputs (e.g., invalid time format).
   - Returns `404 Not Found` if a user doesn’t exist.
   - Returns `409 Conflict` if no free slot is found.

### Heuristics
- **30-Minute Increments**: Balances granularity and performance by checking slots every 30 minutes.
- **Earliest Slot Preference**: Selects the earliest available slot to optimize scheduling.
- **Async Processing**: Uses `@Async` to parallelize conflict checks for multiple users, reducing latency.
- **Database Queries**: Leverages JPA repository methods to minimize database load, querying only relevant events.

### Example
For a request to schedule a 60-minute meeting for `user1`, `user2`, and `user3` from `2024-09-01T09:00:00Z` to `2024-09-05T17:00:00Z`:
- Checks slots (e.g., `09:00-10:00`, `09:30-10:30`).
- Skips `09:00-10:00` due to `user1`’s `Team Sync` event.
- Selects `11:00-12:00` if free for all users.
- Creates events and returns a `201 Created` response.
