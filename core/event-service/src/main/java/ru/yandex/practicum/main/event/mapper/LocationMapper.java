package ru.yandex.practicum.main.event.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.event.LocationDto;
import ru.yandex.practicum.main.event.model.Location;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Component
public class LocationMapper {

    public static Location location(LocationDto dto) {
        Location location = new Location();
        location.setLat(dto.getLat().doubleValue());
        location.setLon(dto.getLon().doubleValue());
        return location;
    }

    public static LocationDto locationDto(Location location) {
        return LocationDto.builder()
                .lat(location.getLat().floatValue())
                .lon(location.getLon().floatValue())
                .build();
    }
}
