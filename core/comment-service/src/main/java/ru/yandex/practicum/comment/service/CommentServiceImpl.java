package ru.yandex.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.comment.mapper.CommentMapper;
import ru.yandex.practicum.comment.model.Comment;
import ru.yandex.practicum.comment.repositiry.CommentRepository;
import ru.yandex.practicum.interaction.client.EventClient;
import ru.yandex.practicum.interaction.client.UserClient;
import ru.yandex.practicum.interaction.dto.*;
import ru.yandex.practicum.interaction.dto.params.CommentSearchParamsAdmin;
import ru.yandex.practicum.interaction.enums.EventState;
import ru.yandex.practicum.interaction.exception.ConflictException;
import ru.yandex.practicum.interaction.exception.NotFoundException;
import ru.yandex.practicum.interaction.exception.ValidationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final EventClient eventClient;
    private final UserClient userClient;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, NewCommentDto dto) {

        UserShortDto userShortDto = userClient.getUserById(userId);
        EventFullDto event = eventClient.getEventForInternalUse(dto.getEventId());

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ValidationException("Can't comment unpublished events");
        }

        Comment comment = CommentMapper.toEntity(dto, userShortDto.getId());
        return CommentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteOwnComment(Long userId, Long commentId) {

        UserShortDto userShortDto = userClient.getUserById(userId);
        Comment comment = getCommentById(commentId);

        if (!comment.getAuthorId().equals(userShortDto.getId())) {
            throw new ConflictException("User can delete only own comments");
        }
        commentRepository.deleteById(commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByEvent(Long eventId, int from, int size) {

        eventClient.getEventForInternalUse(eventId);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").descending());
        return commentRepository.findByEventId(eventId, pageable).stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getUserComments(Long userId, int from, int size) {
        userClient.getUserById(userId);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").descending());
        return commentRepository.findByAuthorId(userId, pageable).stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteByAdmin(Long commentId) {
        getCommentById(commentId);
        commentRepository.deleteById(commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getAllByAdmin(CommentSearchParamsAdmin params) {
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize(),
                Sort.by("createdOn").descending());

        if (params.getAuthorId() != null) {
            userClient.getUserById(params.getAuthorId());
        }

        if (params.getEventId() != null) {
            eventClient.getEventForInternalUse(params.getEventId());
        }

        LocalDateTime rangeStart = null;
        LocalDateTime rangeEnd = null;

        if (params.getRangeStart() != null) {
            try {
                rangeStart = LocalDateTime.parse(params.getRangeStart().replace(" ", "T"));
            } catch (DateTimeParseException e) {
                throw new ValidationException("Invalid rangeStart format, expected yyyy-MM-dd HH:mm:ss");
            }
        }

        if (params.getRangeEnd() != null) {
            try {
                rangeEnd = LocalDateTime.parse(params.getRangeEnd().replace(" ", "T"));
            } catch (DateTimeParseException e) {
                throw new ValidationException("Invalid rangeEnd format, expected yyyy-MM-dd HH:mm:ss");
            }
        }

        return commentRepository.findByFilters(
                        params.getAuthorId(),
                        params.getEventId(),
                        rangeStart,
                        rangeEnd,
                        pageable
                ).stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto updateOwnComment(Long userId, Long commentId, UpdateCommentDto updateDto) {

        userClient.getUserById(userId);
        Comment comment = getCommentById(commentId);

        if (!comment.getAuthorId().equals(userId)) {
            throw new ValidationException("User can update only their own comment");
        }

        comment.setText(updateDto.getText());
        return CommentMapper.toDto(commentRepository.save(comment));
    }

    private Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id " + commentId + " not found"));
    }
}