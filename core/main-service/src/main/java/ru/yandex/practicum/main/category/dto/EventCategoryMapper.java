package ru.yandex.practicum.main.category.dto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.main.category.model.EventCategory;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventCategoryMapper {

    public static EventCategoryDto toCategoryDtoFromCategory(EventCategory category) {
        return new EventCategoryDto(category.getId(), category.getName());
    }

    public static EventCategory toCategoryFromCategoryDto(EventCategoryDto eventCategoryDto) {
        return new EventCategory(-1L, eventCategoryDto.getName());
    }
}
