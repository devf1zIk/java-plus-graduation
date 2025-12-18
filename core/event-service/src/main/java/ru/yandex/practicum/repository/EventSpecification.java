package ru.yandex.practicum.repository;

import org.springframework.data.jpa.domain.Specification;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.model.Event;
import java.time.Instant;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> hasUsers(List<Long> users) {
        return (root, query, criteriaBuilder) -> {
            if (users == null || users.isEmpty()) {
                return criteriaBuilder.conjunction(); // Всегда истинное условие (1=1)
            }
            return root.get("ownerId").in(users);
        };
    }

    public static Specification<Event> hasStates(List<EventState> states) {
        return (root, query, criteriaBuilder) -> {
            if (states == null || states.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("state").in(states);
        };
    }

    public static Specification<Event> hasCategories(List<Long> categories) {
        return (root, query, criteriaBuilder) -> {
            if (categories == null || categories.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("category").get("id").in(categories);
        };
    }

    public static Specification<Event> hasRangeStart(Instant rangeStart) {
        return (root, query, criteriaBuilder) -> {
            if (rangeStart == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("eventDateTime"), rangeStart);
        };
    }

    public static Specification<Event> hasRangeEnd(Instant rangeEnd) {
        return (root, query, criteriaBuilder) -> {
            if (rangeEnd == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("eventDateTime"), rangeEnd);
        };
    }

    public static Specification<Event> hasText(String text) {
        return (root, query, criteriaBuilder) -> {
            if (text == null || text.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            String search = "%" + text.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("annotation")), search),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), search)
            );
        };
    }

    public static Specification<Event> isPaid(Boolean paid) {
        return (root, query, criteriaBuilder) -> {
            if (paid == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("paid"), paid);
        };
    }

    public static Specification<Event> isPublished() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("state"), EventState.PUBLISHED);
    }
}