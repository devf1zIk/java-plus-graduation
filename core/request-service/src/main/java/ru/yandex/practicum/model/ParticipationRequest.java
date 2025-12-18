package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.enums.RequestStatus;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Table(name = "participation_requests")
public class ParticipationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime createdOn;
    @Column(name = "event_id", nullable = false)
    private Long eventId;
    @Column(name = "requester_id", nullable = false)
    private Long requesterId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;
}