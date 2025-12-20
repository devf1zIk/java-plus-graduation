package ru.yandex.practicum.aggregator;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.service.AggregatorProcessor;

@Component
@RequiredArgsConstructor
public class AggregationStart implements CommandLineRunner {

    private final AggregatorProcessor aggregatorProcessor;

    @Override
    public void run(String... args) throws Exception {
        aggregatorProcessor.start();
    }
}