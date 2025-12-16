package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "participation_requests")
public class ParticipationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime createdOn;
    @Column(name = "eventId", nullable = false)
    private long eventId;
    @Column(name = "requesterId", nullable = false)
    private long requesterId;
    @Enumerated(EnumType.STRING)
    private ru.yandex.practicum.enums.RequestStatus status;
}