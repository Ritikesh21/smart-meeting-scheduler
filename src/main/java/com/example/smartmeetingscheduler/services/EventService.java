// src/main/java/com/example/smartmeetingscheduler/services/EventService.java
package com.example.smartmeetingscheduler.services;

import com.example.smartmeetingscheduler.dto.EventDto;
import com.example.smartmeetingscheduler.dto.MeetingResponse;
import com.example.smartmeetingscheduler.dto.ScheduleRequest;
import com.example.smartmeetingscheduler.entities.Event;
import com.example.smartmeetingscheduler.entities.User;
import com.example.smartmeetingscheduler.repositories.EventRepository;
import com.example.smartmeetingscheduler.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<MeetingResponse> scheduleMeeting(ScheduleRequest request) {
        Instant rangeStart;
        Instant rangeEnd;
        try {
            rangeStart = Instant.parse(request.getTimeRange().getStart());
            rangeEnd = Instant.parse(request.getTimeRange().getEnd());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid time format in timeRange");
        }
        long durationMin = request.getDurationMinutes();
        List<String> participantIds = request.getParticipantIds();

        // Validate users exist
        List<User> users = userRepository.findByIdIn(participantIds);
        if (users.size() != participantIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more users not found");
        }

        // Validate time range
        if (rangeStart.isAfter(rangeEnd) || Duration.between(rangeStart, rangeEnd).toMinutes() < durationMin) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid time range or duration");
        }

        // Find available slots
        Instant bestStart = null;
        int maxScore = Integer.MIN_VALUE;
        long stepMin = 15; // Discretization step

        Instant currentStart = rangeStart;
        while (!currentStart.plus(Duration.ofMinutes(durationMin)).isAfter(rangeEnd)) {
            Instant currentEnd = currentStart.plus(Duration.ofMinutes(durationMin));

            // Check availability without lambda
            boolean isAvailable = true;
            for (String userId : participantIds) {
                if (!eventRepository.findOverlapping(userId, currentStart, currentEnd).isEmpty()) {
                    isAvailable = false;
                    break;
                }
            }

            if (isAvailable) {
                int score = computeScore(participantIds, currentStart, currentEnd, rangeStart);
                if (score > maxScore || (score == maxScore && (bestStart == null || currentStart.isBefore(bestStart)))) {
                    maxScore = score;
                    bestStart = currentStart;
                }
            }

            currentStart = currentStart.plus(Duration.ofMinutes(stepMin));
        }

        if (bestStart == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No available time slot found for all participants.");
        }

        Instant bestEnd = bestStart.plus(Duration.ofMinutes(durationMin));

        // Book the meeting
        String meetingId = "meeting-" + UUID.randomUUID();
        for (String userId : participantIds) {
            User user = userRepository.findById(userId).orElseThrow();
            Event newEvent = new Event("New Meeting", bestStart, bestEnd, user);
            eventRepository.save(newEvent);
        }

        MeetingResponse response = new MeetingResponse();
        response.setMeetingId(meetingId);
        response.setParticipantIds(participantIds);
        response.setStartTime(bestStart.toString());
        response.setEndTime(bestEnd.toString());
        return CompletableFuture.completedFuture(response);
    }

    private int computeScore(List<String> participantIds, Instant start, Instant end, Instant rangeStart) {
        int score = 0;

        // Heuristic 1: Prefer earlier slots
        long minFromRangeStart = Duration.between(rangeStart, start).toMinutes();
        score += 100000 - minFromRangeStart;

        // Heuristic 2: Prefer within working hours (9 AM - 5 PM UTC)
        ZonedDateTime startZdt = start.atZone(ZoneId.of("UTC"));
        ZonedDateTime endZdt = end.atZone(ZoneId.of("UTC"));
        if (startZdt.getHour() >= 9 && endZdt.getHour() <= 17 && endZdt.getMinute() <= 0) {
            score += 500;
        }

        // Heuristic 3: Minimize awkward gaps, prefer back-to-back or large gaps
        for (String userId : participantIds) {
            Instant prevEnd = eventRepository.findMaxEndBefore(userId, start);
            Instant nextStart = eventRepository.findMinStartAfter(userId, end);

            if (prevEnd != null) {
                long gapBeforeMin = Duration.between(prevEnd, start).toMinutes();
                if (gapBeforeMin == 0) {
                    score += 100; // Back-to-back bonus
                } else if (gapBeforeMin < 30) {
                    score -= 50; // Penalty for awkward small gap
                } else if (gapBeforeMin >= 60) {
                    score += 50; // Bonus for large gap
                }
            }

            if (nextStart != null) {
                long gapAfterMin = Duration.between(end, nextStart).toMinutes();
                if (gapAfterMin == 0) {
                    score += 100; // Back-to-back bonus
                } else if (gapAfterMin < 30) {
                    score -= 50; // Penalty for awkward small gap
                } else if (gapAfterMin >= 60) {
                    score += 50; // Bonus for large gap
                }
            }
        }

        // Heuristic 4: Prefer slots with 15-minute buffer
        for (String userId : participantIds) {
            Instant prevEnd = eventRepository.findMaxEndBefore(userId, start);
            Instant nextStart = eventRepository.findMinStartAfter(userId, end);
            if (prevEnd != null && Duration.between(prevEnd, start).toMinutes() >= 15) {
                score += 25;
            }
            if (nextStart != null && Duration.between(end, nextStart).toMinutes() >= 15) {
                score += 25;
            }
        }

        return score;
    }

    @Async("taskExecutor")
    public CompletableFuture<List<EventDto>> getUserCalendar(String userId, String startStr, String endStr) {
        Instant start;
        Instant end;
        try {
            start = Instant.parse(startStr);
            end = Instant.parse(endStr);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid time format in query parameters");
        }
        List<Event> events = eventRepository.findByUserIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(userId, start, end);
        List<EventDto> dtos = events.stream().map(event -> {
            EventDto dto = new EventDto();
            dto.setTitle(event.getTitle());
            dto.setStartTime(event.getStartTime().toString());
            dto.setEndTime(event.getEndTime().toString());
            return dto;
        }).collect(Collectors.toList());
        return CompletableFuture.completedFuture(dtos);
    }
}