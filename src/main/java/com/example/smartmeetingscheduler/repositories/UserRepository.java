package com.example.smartmeetingscheduler.repositories;

import com.example.smartmeetingscheduler.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {
    List<User> findByIdIn(List<String> ids);
}