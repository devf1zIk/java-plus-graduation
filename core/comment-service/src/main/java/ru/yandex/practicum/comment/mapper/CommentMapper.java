package ru.yandex.practicum.comment.mapper;


import ru.yandex.practicum.comment.model.Comment;
import ru.yandex.practicum.interaction.dto.CommentDto;
import ru.yandex.practicum.interaction.dto.NewCommentDto;
import java.time.LocalDateTime;

public class CommentMapper {

    public static Comment toEntity(NewCommentDto dto, Long authorId) {
        return Comment.builder()
                .authorId(authorId)
                .eventId(dto.getEventId())
                .text(dto.getText())
                .createdOn(LocalDateTime.now())
                .build();
    }

    public static CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .authorId(comment.getAuthorId())
                .eventId(comment.getEventId())
                .text(comment.getText())
                .createdOn(comment.getCreatedOn())
                .build();
    }
}