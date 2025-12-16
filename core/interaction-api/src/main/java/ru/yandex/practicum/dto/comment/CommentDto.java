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
    Long id;
    EventShortDto event;
    UserShortDto author;
    String text;
    LocalDateTime createdAt;
}
