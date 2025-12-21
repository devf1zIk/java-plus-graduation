package ru.yandex.practicum.comment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.comment.service.CommentService;
import ru.yandex.practicum.interaction.dto.CommentDto;
import ru.yandex.practicum.interaction.dto.params.CommentSearchParamsAdmin;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/comments")
public class AdminCommentController {

    private final CommentService commentService;

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        log.info("AdminCommentController - Deleting comment {}", commentId);
        commentService.deleteByAdmin(commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CommentDto>> getCommentsByAdmin(@ModelAttribute CommentSearchParamsAdmin params) {
        log.info("AdminCommentController - Getting comments with filters: {}", params);
        return ResponseEntity.ok(commentService.getAllByAdmin(params));
    }
}