package ru.yandex.practicum.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCommentRequest {

    @NotNull
    Long eventId;

    @NotBlank
    String text;

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();
}