package ru.yandex.practicum.client.comment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.dto.comment.MergeCommentRequest;
import java.util.Collection;
import java.util.Collections;

@Slf4j
@Component
public class CommentClientFallback implements CommentOperations {

    @Override
    public CommentDto createComment(Long userId, MergeCommentRequest request) {
        log.warn("Comment service unavailable → createComment (userId={})", userId);
        return null;
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        log.warn("Comment service unavailable → deleteComment (userId={}, commentId={})", userId, commentId);
    }

    @Override
    public CommentDto updateComment(Long userId, Long commentId, MergeCommentRequest request) {
        log.warn("Comment service unavailable → updateComment (userId={}, commentId={})", userId, commentId);
        return null;
    }

    @Override
    public Collection<CommentDto> getCommentsByUser(Long userId, int from, int size) {
        log.warn("Comment service unavailable → getCommentsByUser (userId={})", userId);
        return Collections.emptyList();
    }

    @Override
    public Collection<CommentDto> getCommentsByEvent(Long eventId, int from, int size) {
        log.warn("Comment service unavailable → getCommentsByEvent (eventId={})", eventId);
        return Collections.emptyList();
    }

    @Override
    public CommentDto getCommentById(Long commentId) {
        log.warn("Comment service unavailable → getCommentById ({})", commentId);
        return null;
    }
}