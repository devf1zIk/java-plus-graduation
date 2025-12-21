package ru.yandex.practicum.event.service.impl;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.event.mapper.CategoryMapper;
import ru.yandex.practicum.event.model.Category;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.event.model.QEvent;
import ru.yandex.practicum.event.repository.CategoryRepository;
import ru.yandex.practicum.event.repository.EventRepository;
import ru.yandex.practicum.event.service.CategoryService;
import ru.yandex.practicum.interaction.dto.CategoryDto;
import ru.yandex.practicum.interaction.dto.NewCategoryDto;
import ru.yandex.practicum.interaction.exception.ConflictException;
import ru.yandex.practicum.interaction.exception.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;


    @Override
    @Transactional
    public CategoryDto addCategory(NewCategoryDto dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Category with name already exists");
        }

        Category category = Category.builder()
                .name(dto.getName())
                .build();

        return CategoryMapper.toCategoryDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto dto) {
        Category category = getCategoryById(catId);

        String newName = dto.getName();

        if (!category.getName().equals(newName) && categoryRepository.existsByNameAndIdNot(newName, catId)) {
            throw new ConflictException("Category with name already exists");
        }
        category.setName(newName);

        return CategoryMapper.toCategoryDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        getCategoryById(catId);

        BooleanExpression byCategoryId = QEvent.event.category.id.eq(catId);
        List<Event> eventsByCategory = (List<Event>) eventRepository.findAll(byCategoryId);

        if (eventsByCategory.isEmpty()) {
            categoryRepository.deleteById(catId);
        } else {
            throw new ConflictException("The category is not empty");
        }
    }

    @Override
    public List<CategoryDto> getAll(int from, int size) {
        List<Category> categories = categoryRepository
                .findAll(PageRequest.of(from / size, size))
                .getContent();

        return CategoryMapper.toCategoryDtoList(categories);
    }

    @Override
    public CategoryDto getById(Long catId) {
        return categoryRepository.findById(catId)
                .map(CategoryMapper::toCategoryDto)
                .orElseThrow(() -> new NotFoundException("The category with id: " + catId + " not found!"));
    }

    private Category getCategoryById(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("The category with id: " + catId + " not found!"));
    }
}
