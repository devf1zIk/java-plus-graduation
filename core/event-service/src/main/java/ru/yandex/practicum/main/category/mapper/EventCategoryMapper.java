package ru.yandex.practicum.main.category.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.dto.category.CategoryDto;
import ru.yandex.practicum.dto.category.NewCategoryDto;
import ru.yandex.practicum.main.category.model.EventCategory;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventCategoryMapper {

    public static CategoryDto toCategoryDtoFromCategory(EventCategory category) {
        return new CategoryDto(category.getId(), category.getName());
    }

    public static EventCategory toCategoryFromCategoryDto(NewCategoryDto newCategoryDto) {
        return new EventCategory(-1L, newCategoryDto.getName());
    }
}
