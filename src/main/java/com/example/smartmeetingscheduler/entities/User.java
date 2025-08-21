// src/main/java/com/example/smartmeetingscheduler/entities/User.java
package com.example.smartmeetingscheduler.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users") // Changed from default "user" to "users"
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    private String id;
    private String name;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }
}