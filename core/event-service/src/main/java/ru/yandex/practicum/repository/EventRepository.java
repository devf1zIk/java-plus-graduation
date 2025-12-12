package ru.yandex.practicum.repository;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.enums.RequestStatus;
import ru.yandex.practicum.model.Event;
import ru.yandex.practicum.model.EventCategory;
import java.time.Instant;
import java.util.List;

@NotNull
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT p " +
            "FROM Event p " +
            "WHERE p.id IN :events AND p.state = :status")
    List<Event> findAllByEventInAndStatus(List<Long> eventId, RequestStatus status);

    Page<Event> findAllByOwnerId(Long userId, Pageable pageable);

    List<Event> findAllByCategory(EventCategory category);

    @Query("SELECT e FROM Event e " +
            "WHERE e.ownerId is not null or e.ownerId IN :usersIds " +
            "AND e.state is not null or e.state = :states " +
            "AND e.category.id is not null or e.category.id in :categoriesIds " +
            "AND e.eventDateTime > :dateTime")
    Page<Event> findAllEventsAfterDateForUsersByStateAndCategories(List<Long> usersIds, List<EventState> states,
                                                                   List<Long> categoriesIds,
                                                                   Instant dateTime,
                                                                   Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "WHERE e.ownerId IN :usersIds " +
            "AND e.state IN :states " +
            "AND e.category.id IN :categoriesIds " +
            "AND e.eventDateTime BETWEEN :startDateTime AND :endDateTime")
    Page<Event> findAllEventsBetweenDatesForUsersByStateAndCategories(List<Long> usersIds, List<EventState> states,
                                                                      List<Long> categoriesIds,
                                                                      Instant startDateTime,
                                                                      Instant endDateTime, Pageable pageable);

    @Query("""
        SELECT e FROM Event e
        WHERE (:text IS NULL OR (UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%'))
               OR UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%'))))
          AND (:categoriesIds IS NULL OR e.category.id IN :categoriesIds)
          AND (:paid IS NULL OR e.isPaid = :paid)
          AND e.state = :eventState
          AND e.eventDateTime >= :startDateTime
    """)
    Page<Event> findAllAvailablePublishedEventsByCategoryAndStateAfterDate(String text, Instant startDateTime,
                                                                           List<Long> categoriesIds, Pageable pageable,
                                                                           EventState eventState,
                                                                           Boolean paid);

    @Query("""
        SELECT e FROM Event e
        WHERE (:text IS NULL OR (UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%'))
               OR UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%'))))
          AND (:categoriesIds IS NULL OR e.category.id IN :categoriesIds)
          AND (:paid IS NULL OR e.isPaid = :paid)
          AND e.state = :eventState
          AND e.eventDateTime BETWEEN :startDateTime AND :endDateTime
    """)
    Page<Event> findAllAvailablePublishedEventsByCategoryAndStateBetweenDates(String text, Instant startDateTime,
                                                                              Instant endDateTime,
                                                                              List<Long> categoriesIds,
                                                                              Pageable pageable, EventState eventState,
                                                                              Boolean paid);

    @Query("""
        SELECT e FROM Event e
        WHERE (:text IS NULL OR (UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%'))
               OR UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%'))))
          AND (:categoriesIds IS NULL OR e.category.id IN :categoriesIds)
          AND (:paid IS NULL OR e.isPaid = :paid)
          AND e.eventDateTime >= :startDateTime
          AND e.state = :state
    """)
    Page<Event> findAllEventsWithStatusAfterDate(String text, Instant startDateTime,
                                                 List<Long> categoriesIds, EventState state,
                                                 Pageable pageable, Boolean paid);

    @Query("""
        SELECT e FROM Event e
        WHERE (:text IS NULL OR (UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%'))
               OR UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%'))))
          AND (:categoriesIds IS NULL OR e.category.id IN :categoriesIds)
          AND (:paid IS NULL OR e.isPaid = :paid)
          AND e.eventDateTime BETWEEN :startDateTime AND :endDateTime
          AND e.state = :state
    """)
    Page<Event> findAllEventsWithStatusBetweenDates(String text, Instant startDateTime, Instant endDateTime,
                                                    List<Long> categoriesIds, EventState state,
                                                    Pageable pageable, Boolean paid);


}

