package ru.yandex.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.analyzer.model.UserEventWeight;
import java.util.List;

@Repository
public interface UserEventWeightRepository extends JpaRepository<UserEventWeight, Long> {

    @Query("SELECT u.eventId FROM UserEventWeight u WHERE u.userId = :userId")
    List<Long> findEventIdsByUserId(Long userId);

    @Query("SELECT u FROM UserEventWeight u WHERE u.userId = :userId ORDER BY u.timestamp DESC")
    List<UserEventWeight> findRecentByUserId(Long userId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT SUM(u.weight) FROM UserEventWeight u WHERE u.eventId = :eventId")
    Double sumWeightsByEventId(Long eventId);
}
