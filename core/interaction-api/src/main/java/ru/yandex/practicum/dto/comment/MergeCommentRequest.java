package ru.yandex.practicum.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MergeCommentRequest {

    @NotNull
    Long eventId;

    @NotBlank
    String text;

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();
}