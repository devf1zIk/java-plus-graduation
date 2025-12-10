package ru.yandex.practicum.client.comment;

import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.dto.comment.MergeCommentRequest;
import java.util.Collection;

public interface CommentOperations {

    CommentDto createComment(Long userId, MergeCommentRequest request);

    void deleteComment(Long userId, Long commentId);

    CommentDto updateComment(Long userId, Long commentId, MergeCommentRequest request);

    Collection<CommentDto> getCommentsByUser(Long userId, int from, int size);

    Collection<CommentDto> getCommentsByEvent(Long eventId, int from, int size);

    CommentDto getCommentById(Long commentId);
}