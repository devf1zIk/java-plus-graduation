package ru.yandex.practicum.main.category.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.category.CategoryDto;
import ru.yandex.practicum.main.category.service.EventCategoryService;


@RestController
@RequestMapping(path = "/admin/categories")
public class EventCategoryAdminController {
    private final EventCategoryService categoryService;

    @Autowired
    public EventCategoryAdminController(EventCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PatchMapping("/{catId}")
    public CategoryDto update(@RequestBody @Valid CategoryDto eventCategoryDto,
                              @PathVariable Long catId) {
        return categoryService.update(catId, eventCategoryDto);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@RequestBody @Valid CategoryDto eventCategoryDto) {
        return categoryService.create(eventCategoryDto);
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long catId) {
        categoryService.delete(catId);
    }
}

