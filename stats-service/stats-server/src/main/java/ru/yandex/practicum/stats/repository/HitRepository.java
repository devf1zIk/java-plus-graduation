package ru.yandex.practicum.stats.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.dto.StatsDto;
import ru.yandex.practicum.stats.model.Hit;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HitRepository extends JpaRepository<Hit, Integer> {

    @Query("SELECT new ru.yandex.practicum.dto.StatsDto(a.name, h.uri, COUNT (h.id)) " +
            "FROM Hit h JOIN App a ON a.id=h.app.id " +
            "WHERE h.timestamp BETWEEN :startDateTime AND :endDateTime " +
            "GROUP BY  h.uri, a.name " +
            "ORDER BY COUNT(h.id) DESC")
    List<StatsDto> findAllHits(LocalDateTime startDateTime, LocalDateTime endDateTime);

    @Query("SELECT new ru.yandex.practicum.dto.StatsDto(a.name, h.uri, COUNT(DISTINCT h.ip)) " +
            "FROM Hit h JOIN App a ON a.id=h.app.id " +
            "WHERE h.timestamp BETWEEN :startDateTime AND :endDateTime " +
            "GROUP BY a.name, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<StatsDto> findAllUniqueHits(LocalDateTime startDateTime, LocalDateTime endDateTime);

    @Query("SELECT new ru.yandex.practicum.dto.StatsDto(a.name, h.uri, count (h.id))" +
            "FROM  Hit h JOIN App a ON a.id=h.app.id " +
            "WHERE (h.timestamp BETWEEN :startDateTime AND :endDateTime) AND (h.uri IN (:uris))" +
            "GROUP BY h.uri, a.name " +
            "ORDER BY COUNT(h.id) DESC")
    List<StatsDto> findHitsByUris(LocalDateTime startDateTime, LocalDateTime endDateTime, List<String> uris);


    @Query("SELECT new ru.yandex.practicum.dto.StatsDto(a.name, h.uri, COUNT(DISTINCT h.ip)) " +
            "FROM Hit h JOIN App a ON a.id=h.app.id " +
            "WHERE h.timestamp BETWEEN :startDateTime AND :endDateTime AND h.uri IN (:uris) " +
            "GROUP BY a.name, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<StatsDto> findUniqueHitsByUris(LocalDateTime startDateTime, LocalDateTime endDateTime, List<String> uris);
}
