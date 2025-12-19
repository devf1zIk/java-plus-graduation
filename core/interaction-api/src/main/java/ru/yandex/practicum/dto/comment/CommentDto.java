package ru.yandex.practicum.dto.comment;

import lombok.*;
import ru.yandex.practicum.dto.event.EventShortDto;
import ru.yandex.practicum.dto.user.UserShortDto;
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
