package ru.yandex.practicum.dto.event;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventShortForRequestDto {
    private Long id;
    private Long ownerId;
    private Instant publishedOn;
    private Boolean isModerated;
    private Integer participantLimit;
}
