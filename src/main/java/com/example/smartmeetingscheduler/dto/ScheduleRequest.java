package com.example.smartmeetingscheduler.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ScheduleRequest {
    @NotEmpty(message = "Participant IDs cannot be empty")
    private List<String> participantIds;

    @Min(value = 15, message = "Duration must be at least 15 minutes")
    private int durationMinutes;

    @NotNull(message = "Time range is required")
    private TimeRange timeRange;

    @Data
    public static class TimeRange {
        @NotNull(message = "Start time is required")
        private String start;

        @NotNull(message = "End time is required")
        private String end;
    }
}