// src/main/java/com/example/smartmeetingscheduler/SmartMeetingSchedulerApplication.java
package com.example.smartmeetingscheduler;

import com.example.smartmeetingscheduler.entities.Event;
import com.example.smartmeetingscheduler.entities.User;
import com.example.smartmeetingscheduler.repositories.EventRepository;
import com.example.smartmeetingscheduler.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Instant;

@SpringBootApplication
public class SmartMeetingSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartMeetingSchedulerApplication.class, args);
    }

    @Bean
    CommandLineRunner initData(UserRepository userRepository, EventRepository eventRepository) {
        return args -> {
            User alice = new User("user1", "Alice");
            User bob = new User("user2", "Bob");
            User charlie = new User("user3", "Charlie");
            userRepository.save(alice);
            userRepository.save(bob);
            userRepository.save(charlie);

            eventRepository.save(new Event("Team Sync", Instant.parse("2024-09-01T09:00:00Z"), Instant.parse("2024-09-01T10:00:00Z"), alice));
            eventRepository.save(new Event("Lunch Break", Instant.parse("2024-09-01T12:00:00Z"), Instant.parse("2024-09-01T13:00:00Z"), alice));
            eventRepository.save(new Event("Project Review", Instant.parse("2024-09-02T14:00:00Z"), Instant.parse("2024-09-02T15:00:00Z"), bob));
            eventRepository.save(new Event("Client Call", Instant.parse("2024-09-03T10:00:00Z"), Instant.parse("2024-09-03T11:00:00Z"), charlie));
            eventRepository.save(new Event("Daily Standup", Instant.parse("2024-09-04T09:30:00Z"), Instant.parse("2024-09-04T10:00:00Z"), alice));
        };
    }
}