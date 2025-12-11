package ru.yandex.practicum.dto.event;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class LocationDto {

    Long id;

    @NotNull
    private Double lat;
    @NotNull
    private Double lon;
}
