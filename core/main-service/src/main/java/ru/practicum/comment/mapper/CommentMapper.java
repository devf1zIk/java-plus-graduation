package ru.practicum.comment.mapper;

import org.mapstruct.*;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.MergeCommentRequest;
import ru.practicum.comment.model.Comment;
import ru.practicum.event.model.Event;
import ru.practicum.user.model.User;

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