package ru.yandex.practicum.comment.service;


import ru.yandex.practicum.interaction.dto.CommentDto;
import ru.yandex.practicum.interaction.dto.NewCommentDto;
import ru.yandex.practicum.interaction.dto.UpdateCommentDto;
import ru.yandex.practicum.interaction.dto.params.CommentSearchParamsAdmin;

import java.util.List;

public interface CommentService {

    CommentDto createComment(Long userId, NewCommentDto newCommentDto);

    void deleteOwnComment(Long userId, Long commentId);

    List<CommentDto> getCommentsByEvent(Long eventId, int from, int size);

    List<CommentDto> getUserComments(Long userId, int from, int size);

    void deleteByAdmin(Long commentId);

    List<CommentDto> getAllByAdmin(CommentSearchParamsAdmin params);

    CommentDto updateOwnComment(Long userId, Long commentId, UpdateCommentDto updateDto);
}
