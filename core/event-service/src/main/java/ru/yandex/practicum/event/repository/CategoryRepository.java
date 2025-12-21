package ru.yandex.practicum.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.event.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long catId);
}