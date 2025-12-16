package ru.yandex.practicum.main.comment.mapper;

import org.mapstruct.*;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.dto.comment.MergeCommentRequest;
import ru.yandex.practicum.main.comment.model.Comment;
import ru.yandex.practicum.main.event.model.Event;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CommentMapper {
    @Mapping(target = "authorId", source = "userId")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", source = "commentRequest.createdAt")
    Comment requestToComment(MergeCommentRequest commentRequest, Event event, Long userId);

    CommentDto commentToResponse(Comment comment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", source = "event")
    @Mapping(target = "createdAt", source = "commentRequest.createdAt")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateComment(MergeCommentRequest commentRequest, Event event, @MappingTarget Comment comment);
}