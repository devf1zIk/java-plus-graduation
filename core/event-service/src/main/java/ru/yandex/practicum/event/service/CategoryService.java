package ru.yandex.practicum.event.service;

import ru.yandex.practicum.interaction.dto.CategoryDto;
import ru.yandex.practicum.interaction.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {

    CategoryDto addCategory(NewCategoryDto dto);

    CategoryDto updateCategory(Long catId, CategoryDto dto);

    void deleteCategory(Long catId);

    List<CategoryDto> getAll(int from, int size);

    CategoryDto getById(Long catId);
}