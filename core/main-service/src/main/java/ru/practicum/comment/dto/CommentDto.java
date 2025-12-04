package ru.practicum.comment.dto;

import lombok.*;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.user.dto.UserShortDto;
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
