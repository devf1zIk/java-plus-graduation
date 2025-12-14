package ru.yandex.practicum.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.dto.event.CategoryDto;
import ru.yandex.practicum.model.EventCategory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventCategoryMapper {

    public static CategoryDto toCategoryDtoFromCategory(EventCategory category) {
        return new CategoryDto(category.getId(), category.getName());
    }

    public static EventCategory toCategoryFromCategoryDto(CategoryDto categoryDto) {
        return new EventCategory(-1L, categoryDto.getName());
    }
}
