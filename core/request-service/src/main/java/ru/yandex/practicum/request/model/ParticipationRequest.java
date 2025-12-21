package ru.yandex.practicum.request.model;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.interaction.enums.ParticipationRequestStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "participation_requests")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime created;

    @Enumerated(EnumType.STRING)
    private ParticipationRequestStatus status;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;
}