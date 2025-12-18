package ru.yandex.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.model.Event;
import ru.yandex.practicum.model.EventCategory;

import java.time.Instant;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findAllByOwnerId(Long ownerId, Pageable pageable);

    List<Event> findAllByCategory(EventCategory category);

    @Query("SELECT e FROM Event e " +
            "WHERE (:usersIds IS NULL OR e.ownerId IN :usersIds) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categoriesIds IS NULL OR e.category.id IN :categoriesIds) " +
            "AND (:start IS NULL OR e.eventDateTime >= :start)")
    Page<Event> findForAdminAfterDate(
            @Param("usersIds") List<Long> usersIds,
            @Param("states") List<EventState> states,
            @Param("categoriesIds") List<Long> categoriesIds,
            @Param("start") Instant start,
            Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "WHERE (:usersIds IS NULL OR e.ownerId IN :usersIds) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categoriesIds IS NULL OR e.category.id IN :categoriesIds) " +
            "AND e.eventDateTime BETWEEN :start AND :end")
    Page<Event> findForAdminInRange(
            @Param("usersIds") List<Long> usersIds,
            @Param("states") List<EventState> states,
            @Param("categoriesIds") List<Long> categoriesIds,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (:text IS NULL OR " +
            "    UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%')) OR " +
            "    UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%'))) " +
            "AND (:categoriesIds IS NULL OR e.category.id IN :categoriesIds) " +
            "AND (:paid IS NULL OR e.paid = :paid) " +
            "AND e.eventDateTime >= :start")
    Page<Event> findPublishedAfterDate(
            @Param("text") String text,
            @Param("categoriesIds") List<Long> categoriesIds,
            @Param("paid") Boolean paid,
            @Param("start") Instant start,
            Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (:text IS NULL OR " +
            "    UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%')) OR " +
            "    UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%'))) " +
            "AND (:categoriesIds IS NULL OR e.category.id IN :categoriesIds) " +
            "AND (:paid IS NULL OR e.paid = :paid) " +
            "AND e.eventDateTime BETWEEN :start AND :end")
    Page<Event> findPublishedInRange(
            @Param("text") String text,
            @Param("categoriesIds") List<Long> categoriesIds,
            @Param("paid") Boolean paid,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);
}