package ru.yandex.practicum.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.event.LocationDto;
import ru.yandex.practicum.model.Location;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Component
public class LocationMapper {

    public static Location location(LocationDto locationDto) {
        return new Location(
                locationDto.getId(),
                locationDto.getLon(),
                locationDto.getLat()
        );
    }

    public static LocationDto locationDto(Location location) {
        return new LocationDto(
                location.getId(),
                location.getLon(),
                location.getLat()
        );
    }
}
