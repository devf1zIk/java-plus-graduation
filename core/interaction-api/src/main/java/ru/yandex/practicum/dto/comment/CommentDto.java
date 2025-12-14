package ru.yandex.practicum.dto.comment;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {
    Long id;
    Long eventId;
    Long authorId;
    String text;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
