package ru.yandex.practicum.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.entity.UserAction;
import ru.yandex.practicum.repository.UserActionRepository;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserActionServiceImpl implements UserActionService {

    private final UserActionRepository userActionRepository;

    @Override
    public void save(UserAction userAction) {
        Optional<UserAction> maybeUserAction = userActionRepository.findByUserIdAndEventId(userAction.getUserId(), userAction.getEventId());
        if (maybeUserAction.isPresent()) {
            UserAction oldUserAction = maybeUserAction.get();
            if (userAction.getWeight() > oldUserAction.getWeight()) {
                oldUserAction.setWeight(userAction.getWeight());
                userActionRepository.save(oldUserAction);
            }
        } else {
            userActionRepository.save(userAction);
        }
    }
}
