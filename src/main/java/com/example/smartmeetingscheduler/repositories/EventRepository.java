package com.example.smartmeetingscheduler.repositories;

import com.example.smartmeetingscheduler.entities.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e WHERE e.user.id = :userId AND e.startTime < :end AND e.endTime > :start")
    List<Event> findOverlapping(@Param("userId") String userId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT max(e.endTime) FROM Event e WHERE e.user.id = :userId AND e.endTime <= :time")
    Instant findMaxEndBefore(@Param("userId") String userId, @Param("time") Instant time);

    @Query("SELECT min(e.startTime) FROM Event e WHERE e.user.id = :userId AND e.startTime >= :time")
    Instant findMinStartAfter(@Param("userId") String userId, @Param("time") Instant time);

    List<Event> findByUserIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
            String userId, Instant start, Instant end);
}