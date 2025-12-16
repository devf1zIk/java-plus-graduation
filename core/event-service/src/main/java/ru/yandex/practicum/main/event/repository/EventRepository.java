package ru.yandex.practicum.main.event.repository;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.main.category.model.EventCategory;
import ru.yandex.practicum.main.event.model.Event;
import java.time.Instant;
import java.util.List;

@NotNull
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findAllByInitiatorId(long initiatorId, Pageable pageable);

    List<Event> findAllByCategory(EventCategory category);

    @Query("SELECT e FROM Event e " +
            "WHERE (e.initiatorId IS NOT NULL OR e.initiatorId IN :usersIds) " +
            "AND (e.state IS NOT NULL OR e.state IN :states) " +
            "AND e.category.id IN :categoriesIds " +
            "AND e.eventDateTime > :dateTime")
    Page<Event> findAllEventsAfterDateForUsersByStateAndCategories(List<Long> usersIds, List<EventState> states,
                                                                   List<Long> categoriesIds,
                                                                   Instant dateTime,
                                                                   Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "WHERE e.initiatorId IN :usersIds " +
            "AND e.state IN :states " +
            "AND e.category.id IN :categoriesIds " +
            "AND e.eventDateTime BETWEEN :startDateTime AND :endDateTime")
    Page<Event> findAllEventsBetweenDatesForUsersByStateAndCategories(List<Long> usersIds, List<EventState> states,
                                                                      List<Long> categoriesIds,
                                                                      Instant startDateTime,
                                                                      Instant endDateTime, Pageable pageable);

    @Query("""
        SELECT e FROM Event e
        WHERE (:text IS NULL
               OR UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%'))
               OR UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%')))
          AND e.category.id IN :categoriesIds
          AND e.eventDateTime >= :startDateTime
          AND e.state = :eventState
          AND (:paid IS NULL OR e.paid = :paid)
        """)
    Page<Event> findAllAvailablePublishedEventsByCategoryAndStateAfterDate(
            String text,
            Instant startDateTime,
            List<Long> categoriesIds,
            Pageable pageable,
            EventState eventState,
            Boolean paid
    );

    @Query("""
        SELECT e FROM Event e
        WHERE (:text IS NULL
               OR UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%'))
               OR UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%')))
          AND e.category.id IN :categoriesIds
          AND e.eventDateTime BETWEEN :startDateTime AND :endDateTime
          AND e.state = :eventState
          AND (:paid IS NULL OR e.paid = :paid)
        """)
    Page<Event> findAllAvailablePublishedEventsByCategoryAndStateBetweenDates(
            String text,
            Instant startDateTime,
            Instant endDateTime,
            List<Long> categoriesIds,
            Pageable pageable,
            EventState eventState,
            Boolean paid
    );

    @Query("""
        SELECT e FROM Event e
        WHERE (:text IS NULL
               OR UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%'))
               OR UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%')))
          AND e.category.id IN :categoriesIds
          AND (:paid IS NULL OR e.paid = :paid)
          AND e.eventDateTime >= :startDateTime
          AND e.state = :state
        """)
    Page<Event> findAllEventsWithStatusAfterDate(String text, Instant startDateTime,
                                                 List<Long> categoriesIds, EventState state,
                                                 Pageable pageable, Boolean paid);

    @Query("""
        SELECT e FROM Event e
        WHERE (:text IS NULL
               OR UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%'))
               OR UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%')))
          AND e.category.id IN :categoriesIds
          AND (:paid IS NULL OR e.paid = :paid)
          AND e.eventDateTime BETWEEN :startDateTime AND :endDateTime
          AND e.state = :state
        """)
    Page<Event> findAllEventsWithStatusBetweenDates(String text, Instant startDateTime, Instant endDateTime,
                                                    List<Long> categoriesIds, EventState state,
                                                    Pageable pageable, Boolean paid);
}