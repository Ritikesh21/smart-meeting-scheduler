package com.example.smartmeetingscheduler.dto;

import lombok.Data;

import java.util.List;

@Data
public class MeetingResponse {
    private String meetingId;
    private String title = "New Meeting";
    private List<String> participantIds;
    private String startTime;
    private String endTime;
}