package ru.yandex.practicum.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import ru.yandex.practicum.enums.EventState;
import java.time.Instant;

@Entity
@Table(name = "events")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Size(min = 1, max = 120)
    private String title;
    @Size(min = 20, max = 2000)
    private String annotation;
    @Size(min = 20, max = 7000)
    private String description;
    @ManyToOne(fetch = FetchType.EAGER)
    @JsonIgnore
    @JoinColumn(name = "category_id")
    private EventCategory category;
    private Instant createdOn;
    private Instant eventDateTime;

    @Column(name = "owner_id")
    private Long ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(nullable = false)
    @Builder.Default
    Boolean paid = false;

    @Column(name = "initiator_id", nullable = false)
    Long initiatorId;

    @Column(name = "participant_limit", nullable = false)
    Integer participantLimit;

    @Column(name = "published_on")
    private Instant publishedOn;

    @Column(name = "is_moderated")
    @Builder.Default
    private Boolean isModerated = true;

    @Enumerated(EnumType.STRING)
    private EventState state;
}
