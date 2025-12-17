package ru.yandex.practicum.main.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.category.CategoryDto;
import ru.yandex.practicum.dto.category.NewCategoryDto;
import ru.yandex.practicum.exception.model.AlreadyExistsException;
import ru.yandex.practicum.exception.model.ConflictException;
import ru.yandex.practicum.exception.model.NotFoundException;
import ru.yandex.practicum.main.category.mapper.EventCategoryMapper;
import ru.yandex.practicum.main.category.model.EventCategory;
import ru.yandex.practicum.main.category.repository.EventCategoryRepository;
import ru.yandex.practicum.main.event.repository.EventRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventCategoryService {

    private final EventCategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    public List<CategoryDto> getAllCategories(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return categoryRepository.findAll(pageable)
                .stream()
                .map(EventCategoryMapper::toCategoryDtoFromCategory)
                .toList();
    }

    public CategoryDto getCategoryById(Long categoryId) {
        EventCategory category = getCategoryIfExist(categoryId);
        return EventCategoryMapper.toCategoryDtoFromCategory(category);
    }

    public CategoryDto updateCategory(Long categoryId, NewCategoryDto dto) {
        EventCategory category = getCategoryIfExist(categoryId);

        String newName = dto.getName().trim();

        if (categoryRepository.existsByNameAndIdNot(newName, categoryId)) {
            throw new AlreadyExistsException("Категория с именем '" + newName + "' уже существует");
        }

        category.setName(newName);
        EventCategory updated = categoryRepository.save(category);

        return EventCategoryMapper.toCategoryDtoFromCategory(updated);
    }

    public CategoryDto addCategory(NewCategoryDto dto) {
        String name = dto.getName().trim();

        if (categoryRepository.existsByName(name)) {
            throw new AlreadyExistsException("Категория с именем '" + name + "' уже существует");
        }

        EventCategory category = new EventCategory();
        category.setName(name);
        EventCategory saved = categoryRepository.save(category);

        return EventCategoryMapper.toCategoryDtoFromCategory(saved);
    }

    public void deleteCategory(Long categoryId) {
        EventCategory category = getCategoryIfExist(categoryId);

        if (eventRepository.findByCategoryId(categoryId)) {
            throw new ConflictException("Нельзя удалить категорию, у которой есть связанные события");
        }
        categoryRepository.delete(category);
    }

    private EventCategory getCategoryIfExist(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + categoryId + " не существует"));
    }
}