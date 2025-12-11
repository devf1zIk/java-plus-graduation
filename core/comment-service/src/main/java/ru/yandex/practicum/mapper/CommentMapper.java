package ru.yandex.practicum.mapper;

import org.mapstruct.*;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.dto.comment.MergeCommentRequest;
import ru.yandex.practicum.model.Comment;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CommentMapper {
    @Mapping(target = "authorId", source = "userId")
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", source = "commentRequest.createdAt")
    Comment requestToComment(MergeCommentRequest commentRequest, Long eventId, Long userId);

    CommentDto commentToResponse(Comment comment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "createdAt", source = "commentRequest.createdAt")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateComment(MergeCommentRequest commentRequest, Long eventId, @MappingTarget Comment comment);
}