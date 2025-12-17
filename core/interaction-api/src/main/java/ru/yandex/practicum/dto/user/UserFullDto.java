package ru.yandex.practicum.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserFullDto {
    @NotEmpty
    @Email
    @Size(min = 6, max = 254)
    String email;
    long id;
    @NotBlank
    @Size(min = 2, max = 250)
    String name;
}