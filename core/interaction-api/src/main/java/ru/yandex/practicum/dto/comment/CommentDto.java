package ru.yandex.practicum.dto.comment;

import jakarta.persistence.Column;
import lombok.*;
import ru.yandex.practicum.dto.event.EventShortDto;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {
    private Long id;
    private EventShortDto event;
    private Long authorId;
    private String text;
    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
