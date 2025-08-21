package com.example.smartmeetingscheduler.dto;

import lombok.Data;

@Data
public class EventDto {
    private String title;
    private String startTime;
    private String endTime;
}