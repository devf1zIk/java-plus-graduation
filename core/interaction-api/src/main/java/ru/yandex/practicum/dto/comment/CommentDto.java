package ru.yandex.practicum.dto.comment;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.dto.event.EventShortDto;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentDto {
    Long id;
    EventShortDto event;
    Long authorId;
    String text;
    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();
}
