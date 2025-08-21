// src/test/java/com/example/smartmeetingscheduler/EventServiceTest.java
package com.example.smartmeetingscheduler;

import com.example.smartmeetingscheduler.dto.MeetingResponse;
import com.example.smartmeetingscheduler.dto.ScheduleRequest;
import com.example.smartmeetingscheduler.entities.Event;
import com.example.smartmeetingscheduler.entities.User;
import com.example.smartmeetingscheduler.repositories.EventRepository;
import com.example.smartmeetingscheduler.repositories.UserRepository;
import com.example.smartmeetingscheduler.services.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EventServiceTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        userRepository.deleteAll();

        User user1 = new User("test1", "Test User 1");
        userRepository.save(user1);
        eventRepository.save(new Event("Existing Meeting", Instant.parse("2024-09-01T10:00:00Z"), Instant.parse("2024-09-01T11:00:00Z"), user1));
    }

    @Test
    void testScheduleMeetingSuccess() throws InterruptedException, ExecutionException {
        ScheduleRequest request = new ScheduleRequest();
        request.setParticipantIds(List.of("test1"));
        request.setDurationMinutes(60);
        ScheduleRequest.TimeRange timeRange = new ScheduleRequest.TimeRange();
        timeRange.setStart("2024-09-01T09:00:00Z");
        timeRange.setEnd("2024-09-01T17:00:00Z");
        request.setTimeRange(timeRange);

        CompletableFuture<MeetingResponse> future = eventService.scheduleMeeting(request);
        MeetingResponse response = future.get(); // Wait for async completion

        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getMeetingId(), "Meeting ID should not be null");
        assertEquals("test1", response.getParticipantIds().get(0), "Participant ID should match");
        assertEquals("2024-09-01T11:00:00Z", response.getStartTime(), "Start time should be after existing meeting");
        assertEquals("2024-09-01T12:00:00Z", response.getEndTime(), "End time should be 60 minutes after start");
    }

    @Test
    void testScheduleMeetingNoSlot() {
        ScheduleRequest request = new ScheduleRequest();
        request.setParticipantIds(List.of("test1"));
        request.setDurationMinutes(60);
        ScheduleRequest.TimeRange timeRange = new ScheduleRequest.TimeRange();
        timeRange.setStart("2024-09-01T10:00:00Z");
        timeRange.setEnd("2024-09-01T11:00:00Z");
        request.setTimeRange(timeRange);

        assertThrows(ExecutionException.class, () -> {
            eventService.scheduleMeeting(request).get();
        }, "Should throw exception when no slot is available");
    }

    @Test
    void testGetUserCalendar() throws InterruptedException, ExecutionException {
        CompletableFuture<List<com.example.smartmeetingscheduler.dto.EventDto>> future = eventService.getUserCalendar(
                "test1", "2024-09-01T00:00:00Z", "2024-09-01T23:59:59Z");
        List<com.example.smartmeetingscheduler.dto.EventDto> events = future.get();

        assertNotNull(events, "Events list should not be null");
        assertEquals(1, events.size(), "Should return one event");
        assertEquals("Existing Meeting", events.get(0).getTitle(), "Event title should match");
        assertEquals("2024-09-01T10:00:00Z", events.get(0).getStartTime(), "Event start time should match");
        assertEquals("2024-09-01T11:00:00Z", events.get(0).getEndTime(), "Event end time should match");
    }
}