package ru.yandex.practicum.main.comment.dto;

import lombok.*;
import ru.yandex.practicum.main.event.dto.EventShortDto;
import ru.yandex.practicum.main.user.dto.UserShortDto;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {
    private Long id;
    private EventShortDto event;
    private UserShortDto author;
    private String text;
    private LocalDateTime createdAt;
}
