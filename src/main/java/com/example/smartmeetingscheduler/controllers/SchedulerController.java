package com.example.smartmeetingscheduler.controllers;

import com.example.smartmeetingscheduler.dto.EventDto;
import com.example.smartmeetingscheduler.dto.MeetingResponse;
import com.example.smartmeetingscheduler.dto.ScheduleRequest;
import com.example.smartmeetingscheduler.services.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class SchedulerController {

    @Autowired
    private EventService eventService;

    @PostMapping("/schedule")
    @ResponseStatus(HttpStatus.CREATED)
    public CompletableFuture<MeetingResponse> schedule(@Valid @RequestBody ScheduleRequest request) {
        return eventService.scheduleMeeting(request);
    }

    @GetMapping("/users/{userId}/calendar")
    public CompletableFuture<List<EventDto>> getCalendar(@PathVariable String userId,
                                                        @RequestParam("start") String start,
                                                        @RequestParam("end") String end) {
        return eventService.getUserCalendar(userId, start, end);
    }
}