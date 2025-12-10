package ru.yandex.practicum.client.comment;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.dto.comment.MergeCommentRequest;
import java.util.Collection;

@FeignClient(name = "comment-service",fallback = CommentClientFallback.class)
public interface CommentClient extends CommentOperations {

    @Override
    @PostMapping("/users/{userId}/comments")
    CommentDto createComment(@PathVariable("userId") Long userId,
                             @RequestBody MergeCommentRequest request);

    @Override
    @DeleteMapping("/users/{userId}/comments/{commentId}")
    void deleteComment(@PathVariable("userId") Long userId,
                       @PathVariable("commentId") Long commentId);

    @Override
    @PatchMapping("/users/{userId}/comments/{commentId}")
    CommentDto updateComment(@PathVariable("userId") Long userId,
                             @PathVariable("commentId") Long commentId,
                             @RequestBody MergeCommentRequest request);

    @Override
    @GetMapping("/users/{userId}/comments")
    Collection<CommentDto> getCommentsByUser(@PathVariable("userId") Long userId,
                                             @RequestParam("from") int from,
                                             @RequestParam("size") int size);

    @Override
    @GetMapping("/comments/{commentId}")
    CommentDto getCommentById(@PathVariable("commentId") Long commentId);

    @Override
    @GetMapping("/events/{eventId}/comments")
    Collection<CommentDto> getCommentsByEvent(@PathVariable("eventId") Long eventId,
                                              @RequestParam("from") int from,
                                              @RequestParam("size") int size);
}