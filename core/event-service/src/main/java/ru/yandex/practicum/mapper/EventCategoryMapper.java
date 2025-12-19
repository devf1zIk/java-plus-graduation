package ru.yandex.practicum.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.dto.category.EventCategoryDto;
import ru.yandex.practicum.model.EventCategory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventCategoryMapper {

    public static EventCategoryDto toCategoryDtoFromCategory(EventCategory category) {
        return new EventCategoryDto(category.getId(), category.getName());
    }

    public static EventCategory toCategoryFromCategoryDto(EventCategoryDto eventCategoryDto) {
        return new EventCategory(-1L, eventCategoryDto.getName());
    }
}
