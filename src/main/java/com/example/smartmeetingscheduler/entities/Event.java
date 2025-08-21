// src/main/java/com/example/smartmeetingscheduler/entities/Event.java
package com.example.smartmeetingscheduler.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private Instant startTime;
    private Instant endTime;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Event(String title, Instant startTime, Instant endTime, User user) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.user = user;
    }
}