package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.dto.comment.MergeCommentRequest;
import ru.yandex.practicum.exception.model.NotFoundException;
import ru.yandex.practicum.exception.model.PublicationException;
import ru.yandex.practicum.feign.event.EventClient;
import ru.yandex.practicum.feign.user.UserClient;
import ru.yandex.practicum.mapper.CommentMapper;
import ru.yandex.practicum.model.Comment;
import ru.yandex.practicum.repository.CommentRepository;
import java.util.Collection;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final UserClient userClient;
    private final EventClient eventClient;

    public CommentDto createComment(MergeCommentRequest request, Long userId) {
        userClient.getById(userId);

        var event = eventClient.getById(request.getEventId());
        if (event.getPublishedOn() == null) {
            throw new PublicationException("Event must be published to comment");
        }

        Comment comment = commentMapper.requestToComment(request, request.getEventId(), userId);
        comment.setCreatedAt(java.time.LocalDateTime.now());

        Comment saved = commentRepository.save(comment);
        CommentDto response = commentMapper.commentToResponse(saved);
        log.info("Comment id={} created by user id={}", response.getId(), userId);
        return response;
    }

    public void deleteCommentByIdAndAuthor(Long commentId, Long userId) {
        userClient.getById(userId);

        if (commentRepository.deleteCommentByIdAndAuthorId(commentId, userId) == 0) {
            throw new NotFoundException("Comment not found or not owned by user");
        }
        log.info("Comment id={} deleted by user id={}", commentId, userId);
    }

    public CommentDto updateCommentByIdAndAuthorId(Long commentId, Long userId, MergeCommentRequest request) {
        userClient.getById(userId);

        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Comment not found or not owned by user"));

        if (!comment.getEventId().equals(request.getEventId())) {
            throw new IllegalArgumentException("Cannot change eventId of comment");
        }

        eventClient.getById(request.getEventId());

        commentMapper.updateComment(request, comment);

        Comment saved = commentRepository.save(comment);
        CommentDto response = commentMapper.commentToResponse(saved);
        log.info("Comment id={} updated by user id={}", response.getId(), userId);
        return response;
    }

    public Collection<CommentDto> getAllCommentsByUser(Long userId, Integer from, Integer size) {
        userClient.getById(userId);
        Pageable pageable = createPageable(from, size);
        return commentRepository.findAllByAuthorIdOrderByCreatedAtDesc(userId, pageable)
                .stream()
                .map(commentMapper::commentToResponse)
                .toList();
    }

    public Collection<CommentDto> getAllCommentsByEvent(Long eventId, Integer from, Integer size) {
        eventClient.getById(eventId); // проверка существования
        Pageable pageable = createPageable(from, size);
        return commentRepository.findAllByEventIdOrderByCreatedAtDesc(eventId, pageable)
                .stream()
                .map(commentMapper::commentToResponse)
                .toList();
    }

    public Collection<CommentDto> getAllCommentsByUserAndEvent(Long userId, Long eventId, Integer from, Integer size) {
        userClient.getById(userId);
        eventClient.getById(eventId);
        Pageable pageable = createPageable(from, size);
        return commentRepository.findAllByAuthorIdAndEventIdOrderByCreatedAtDesc(userId, eventId, pageable)
                .stream()
                .map(commentMapper::commentToResponse)
                .toList();
    }

    public CommentDto getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " not found"));
        return commentMapper.commentToResponse(comment);
    }

    private Pageable createPageable(Integer from, Integer size) {
        return PageRequest.of(from / size, size);
    }
}