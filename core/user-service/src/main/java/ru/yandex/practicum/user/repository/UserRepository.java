package ru.yandex.practicum.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.user.model.User;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findAllByIdIn(List<Long> ids);

    boolean existsByEmail(String email);
}