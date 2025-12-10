package ru.yandex.practicum.mapper;

import org.mapstruct.*;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.dto.comment.MergeCommentRequest;
import ru.yandex.practicum.model.Comment;
import ru.yandex.practicum.model.Event;
import ru.yandex.practicum.model.User;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CommentMapper {
    @Mapping(target = "author", source = "user")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", source = "commentRequest.createdAt")
    Comment requestToComment(MergeCommentRequest commentRequest, Event event, User user);

    CommentDto commentToResponse(Comment comment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", source = "event")
    @Mapping(target = "createdAt", source = "commentRequest.createdAt")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateComment(MergeCommentRequest commentRequest, Event event, @MappingTarget Comment comment);
}