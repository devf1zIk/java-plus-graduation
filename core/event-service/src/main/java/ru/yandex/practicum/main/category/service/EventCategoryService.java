package ru.yandex.practicum.main.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.category.CategoryDto;
import ru.yandex.practicum.dto.category.NewCategoryDto;
import ru.yandex.practicum.exception.model.ConflictException;
import ru.yandex.practicum.exception.model.NotFoundException;
import ru.yandex.practicum.main.category.mapper.EventCategoryMapper;
import ru.yandex.practicum.main.category.model.EventCategory;
import ru.yandex.practicum.main.category.repository.EventCategoryRepository;
import ru.yandex.practicum.main.event.model.Event;
import ru.yandex.practicum.main.event.repository.EventRepository;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventCategoryService {

    private final EventCategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    public List<CategoryDto> getAllCategories(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return categoryRepository.findAll(pageable).stream()
                .map(EventCategoryMapper::toCategoryDtoFromCategory).toList();
    }

    public CategoryDto getCategoryById(Long categoryId) {
        EventCategory category = getCategoryIfExist(categoryId);
        return EventCategoryMapper.toCategoryDtoFromCategory(category);
    }

    public CategoryDto updateCategory(Long categoryId, NewCategoryDto newCategoryDto) {
        EventCategory categoryToUpdate = getCategoryIfExist(categoryId);
        Optional<EventCategory> categoryWithSameName = categoryRepository.findByName(newCategoryDto.getName());
        if (categoryWithSameName.isPresent() && !categoryWithSameName.get().getId().equals(categoryToUpdate.getId())) {
            throw new ConflictException("Категория " + newCategoryDto.getName() + " уже существует!");
        }
        categoryToUpdate.setName(newCategoryDto.getName());
        EventCategory updatedCategory = categoryRepository.save(categoryToUpdate);

        return EventCategoryMapper.toCategoryDtoFromCategory(updatedCategory);
    }

    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        Optional<EventCategory> categoryWithSameName = categoryRepository.findByName(newCategoryDto.getName());
        if (categoryWithSameName.isPresent()) {
            throw new ConflictException("Категория " + newCategoryDto.getName() + " уже существует!");
        }

        EventCategory category = categoryRepository.save(EventCategoryMapper.toCategoryFromCategoryDto(newCategoryDto));
        return EventCategoryMapper.toCategoryDtoFromCategory(category);
    }

    public void deleteCategory(Long categoryId) {
        EventCategory category = getCategoryIfExist(categoryId);
        List<Event> eventsList = eventRepository.findAllByCategory(category);
        if (!eventsList.isEmpty()) {
            throw new ConflictException("У категории есть события. Удаление невозможно!");
        }
        categoryRepository.delete(category);
    }

    private EventCategory getCategoryIfExist(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + categoryId + " не существует!"));
    }
}
