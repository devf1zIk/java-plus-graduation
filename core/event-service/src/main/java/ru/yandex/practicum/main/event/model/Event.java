package ru.yandex.practicum.main.event.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.main.category.model.EventCategory;
import java.time.Instant;

@Entity
@Table(name = "events")
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Size(min = 1, max = 120)
    private String title;
    @Size(min = 20, max = 2000)
    @Column(name = "annotation", nullable = false, columnDefinition = "TEXT")
    private String annotation;
    @Size(min = 20, max = 7000)
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;
    @ManyToOne(fetch = FetchType.EAGER)
    @JsonIgnore
    @JoinColumn(name = "category_id")
    private EventCategory category;
    private Instant createdOn;
    private Instant eventDateTime;
    @Column(name = "initiator_id")
    private Long initiatorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;
    private Boolean paid;
    private Long participantLimit;
    private Instant publishedOn;
    private Boolean isModerated;
    @Enumerated(EnumType.STRING)
    private EventState state;
}
