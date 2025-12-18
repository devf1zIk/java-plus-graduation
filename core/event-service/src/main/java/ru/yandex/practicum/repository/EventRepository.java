package ru.yandex.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.model.Event;
import ru.yandex.practicum.model.EventCategory;
import java.time.Instant;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Page<Event> findAllByOwnerId(Long ownerId, Pageable pageable);

    List<Event> findAllByCategory(EventCategory category);

    @Query("SELECT e FROM Event e WHERE e.state = 'PUBLISHED' AND e.eventDateTime >= :start")
    Page<Event> findAllPublishedAfter(@Param("start") Instant start, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.state = 'PUBLISHED' AND e.eventDateTime BETWEEN :start AND :end")
    Page<Event> findAllPublishedInRange(@Param("start") Instant start, @Param("end") Instant end, Pageable pageable);
}