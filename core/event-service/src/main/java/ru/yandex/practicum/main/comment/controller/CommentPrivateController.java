package ru.yandex.practicum.main.comment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.dto.comment.MergeCommentRequest;
import ru.yandex.practicum.main.comment.service.CommentService;
import java.util.Collection;

@RestController
@RequestMapping("/users/{userId}/comments")
@AllArgsConstructor
@Validated
public class CommentPrivateController {
    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@Valid @RequestBody MergeCommentRequest request, @PathVariable @Positive Long userId) {
        return commentService.createComment(request, userId);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable @Positive Long commentId, @PathVariable @Positive Long userId) {
        commentService.deleteCommentByIdAndAuthor(commentId, userId);
    }

    @PatchMapping("/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto updateComment(@Valid @RequestBody MergeCommentRequest request,
                                    @PathVariable @Positive Long userId,
                                    @PathVariable @Positive Long commentId) {
        return commentService.updateCommentByIdAndAuthorId(commentId, userId, request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<CommentDto> getAllCommentsByUser(@PathVariable @Positive Long userId,
                                                   @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                                   @RequestParam(defaultValue = "10") @Positive Integer size) {
        return commentService.getAllCommentsByUser(userId, from, size);
    }

    @GetMapping("events/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public Collection<CommentDto> getAllCommentsByEvent(@PathVariable @Positive Long eventId,
                                                    @PathVariable @Positive Long userId,
                                                    @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                                    @RequestParam(defaultValue = "10") @Positive Integer size) {
        return commentService.getAllCommentsByUserAndEvent(userId, eventId, from, size);
    }
}