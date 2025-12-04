package ru.practicum.category.dto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.category.model.EventCategory;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventCategoryMapper {

    public static EventCategoryDto toCategoryDtoFromCategory(EventCategory category) {
        return new EventCategoryDto(category.getId(), category.getName());
    }

    public static EventCategory toCategoryFromCategoryDto(EventCategoryDto eventCategoryDto) {
        return new EventCategory(-1L, eventCategoryDto.getName());
    }
}
