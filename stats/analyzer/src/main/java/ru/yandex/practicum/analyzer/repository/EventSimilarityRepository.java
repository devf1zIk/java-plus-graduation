package ru.yandex.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.analyzer.model.EventSimilarity;
import java.util.List;

@Repository
public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    @Query("SELECT e FROM EventSimilarity e WHERE e.eventA = :eventId OR e.eventB = :eventId")
    List<EventSimilarity> findByEvent(Long eventId);
}
