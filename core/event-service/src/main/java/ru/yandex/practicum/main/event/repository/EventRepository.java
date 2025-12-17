package ru.yandex.practicum.main.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.main.event.model.Event;
import java.time.Instant;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    @Query("""
        SELECT e FROM Event e
        WHERE (:users IS NULL OR e.initiatorId IN :users)
          AND (:states IS NULL OR e.state IN :states)
          AND (:categories IS NULL OR e.category.id IN :categories)
          AND e.eventDateTime > :dateTime
        """)
    Page<Event> findAllEventsAfterDateForUsersByStateAndCategories(
            List<Long> users,
            List<EventState> states,
            List<Long> categories,
            Instant dateTime,
            Pageable pageable
    );

    @Query("""
        SELECT e FROM Event e
        WHERE (:users IS NULL OR e.initiatorId IN :users)
          AND (:states IS NULL OR e.state IN :states)
          AND (:categories IS NULL OR e.category.id IN :categories)
          AND e.eventDateTime BETWEEN :start AND :end
        """)
    Page<Event> findAllEventsBetweenDatesForUsersByStateAndCategories(
            List<Long> users,
            List<EventState> states,
            List<Long> categories,
            Instant start,
            Instant end,
            Pageable pageable
    );

    @Query("""
        SELECT e FROM Event e
        WHERE (e.participantLimit = 0 OR e.participantLimit > e.confirmedRequests)
          AND e.state = :state
          AND (:text IS NULL\s
               OR UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%'))
               OR UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%')))
          AND (:categories IS NULL OR e.category.id IN :categories)
          AND (:paid IS NULL OR e.paid = :paid)
          AND e.eventDateTime >= :start
       \s""")
    Page<Event> findAllAvailablePublishedEventsByCategoryAndStateAfterDate(
            String text,
            Instant start,
            List<Long> categories,
            Boolean paid,
            EventState state,
            Pageable pageable
    );

    @Query("""
        SELECT e FROM Event e
        WHERE (e.participantLimit = 0 OR e.participantLimit > e.confirmedRequests)
          AND e.state = :state
          AND (:text IS NULL OR UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%'))
               OR UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%')))
          AND (:categories IS NULL OR e.category.id IN :categories)
          AND (:paid IS NULL OR e.paid = :paid)
          AND e.eventDateTime BETWEEN :start AND :end
        """)
    Page<Event> findAllAvailablePublishedEventsByCategoryAndStateBetweenDates(
            String text,
            Instant start,
            Instant end,
            List<Long> categories,
            Boolean paid,
            EventState state,
            Pageable pageable
    );

    @Query("""
        SELECT e FROM Event e
        WHERE e.state = :state
          AND (:text IS NULL\s
               OR UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%'))
               OR UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%')))
          AND (:categories IS NULL OR e.category.id IN :categories)
          AND (:paid IS NULL OR e.paid = :paid)
          AND e.eventDateTime >= :start
       \s""")
    Page<Event> findAllEventsWithStatusAfterDate(
            String text,
            Instant start,
            List<Long> categories,
            Boolean paid,
            EventState state,
            Pageable pageable
    );

    @Query("""
        SELECT e FROM Event e
        WHERE e.state = :state
          AND (:text IS NULL\s
               OR UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%'))
               OR UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%')))
          AND (:categories IS NULL OR e.category.id IN :categories)
          AND (:paid IS NULL OR e.paid = :paid)
          AND e.eventDateTime BETWEEN :start AND :end
       \s""")
    Page<Event> findAllEventsWithStatusBetweenDates(
            String text,
            Instant start,
            Instant end,
            List<Long> categories,
            Boolean paid,
            EventState state,
            Pageable pageable
    );
}