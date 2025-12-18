package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.enums.RequestStatus;
import ru.yandex.practicum.model.ParticipationRequest;
import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByEventId(Long eventId);

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    Boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    List<ParticipationRequest> findAllByIdIn(List<Long> ids);

    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("SELECT pr.eventId, COUNT(pr) " +
            "FROM ParticipationRequest pr " +
            "WHERE pr.eventId IN :eventIds AND pr.status = 'CONFIRMED' " +
            "GROUP BY pr.eventId")
    List<Object[]> countConfirmedByEventIdsRaw(@Param("eventIds") List<Long> eventIds);
}
