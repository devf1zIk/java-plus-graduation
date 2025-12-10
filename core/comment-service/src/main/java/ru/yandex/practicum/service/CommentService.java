package ru.yandex.practicum.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.dto.comment.MergeCommentRequest;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.exception.model.NotFoundException;
import ru.yandex.practicum.exception.model.PublicationException;
import ru.yandex.practicum.mapper.CommentMapper;
import ru.yandex.practicum.model.Comment;
import ru.yandex.practicum.model.Event;
import ru.yandex.practicum.model.User;
import ru.yandex.practicum.repository.CommentRepository;
import ru.yandex.practicum.repository.EventRepository;
import ru.yandex.practicum.repository.UserRepository;
import java.util.Collection;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public CommentDto createComment(MergeCommentRequest mergeCommentRequest, Long userId) {
        User user = findUserById(userId);
        Event event = findEventById(mergeCommentRequest.getEventId());

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new PublicationException("Event must be published");
        }

        Comment comment = commentMapper.requestToComment(mergeCommentRequest, event, user);
        CommentDto response = commentMapper.commentToResponse(commentRepository.save(comment));
        log.info("Comment id={} was created by user id={}", response.getId(), response.getAuthor().getId());
        return response;
    }

    public void deleteCommentByIdAndAuthor(Long commentId, Long userId) {
        if (commentRepository.deleteCommentByIdAndAuthorId(commentId, userId) != 0) {
            log.info("Comment with id={} was deleted by user id={}", commentId, userId);
        } else {
            throw new NotFoundException(String.format("Comment with id=%d by author id=%d was not found", commentId, userId));
        }
    }

    public CommentDto updateCommentByIdAndAuthorId(Long commentId, Long userId, MergeCommentRequest request) {
        Comment oldComment = commentRepository.findByIdAndAuthorId(commentId, userId).orElseThrow(() ->
                new NotFoundException(String.format("Comment with id=%d by author id=%d was not found", commentId, userId)));

        if (!oldComment.getEvent().getId().equals(request.getEventId())) {
            throw new DataIntegrityViolationException("Event Id not correct");
        }

        commentMapper.updateComment(
                request,
                findEventById(request.getEventId()),
                oldComment);

        CommentDto response = commentMapper.commentToResponse(commentRepository.save(oldComment));
        log.info("Comment id={} was updated by user id={}", response.getId(), response.getAuthor().getId());
        return response;
    }

    public Collection<CommentDto> getAllCommentsByUser(Long userId, Integer from, Integer size) {
        log.info("Get all comments for user id={}", userId);
        return commentRepository.findAllByAuthorIdOrderByCreatedAtDesc(userId, createPageable(from, size))
                .stream()
                .map(commentMapper::commentToResponse)
                .toList();
    }

    public Collection<CommentDto> getAllCommentsByEvent(Long eventId, Integer from, Integer size) {
        log.info("Get all comments for event id={}", eventId);
        return commentRepository.findAllByEventIdOrderByCreatedAtDesc(eventId, createPageable(from, size))
                .stream()
                .map(commentMapper::commentToResponse)
                .toList();
    }

    public Collection<CommentDto> getAllCommentsByUserAndEvent(Long userId, Long eventId, Integer from, Integer size) {
        log.info("Get all comments for event id={} and user id={}", eventId, userId);
        return commentRepository.findAllByAuthorIdAndEventIdOrderByCreatedAtDesc(userId, eventId, createPageable(from, size))
                .stream()
                .map(commentMapper::commentToResponse)
                .toList();
    }

    public CommentDto getCommentById(Long commentId) {
        log.info("Get comment with id={}", commentId);
        return commentMapper.commentToResponse(commentRepository.findById(commentId).orElseThrow(() ->
                new NotFoundException(String.format("Comment with id=%d was not found", commentId))));
    }

    private Pageable createPageable(Integer from, Integer size) {
        int pageNumber = from / size;
        return PageRequest.of(pageNumber, size);
    }

    private Event findEventById(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() ->
                new NotFoundException(String.format("Event with id=%d not found", eventId)));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException(String.format("User with id=%d not found", userId)));
    }
}
