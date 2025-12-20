package ru.yandex.practicum;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import ru.yandex.practicum.processors.SimilarityProcessor;
import ru.yandex.practicum.processors.UserActionProcessor;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
@RequiredArgsConstructor
public class AnalyzerApplication implements CommandLineRunner {

    private final SimilarityProcessor similarityProcessor;
    private final UserActionProcessor userActionProcessor;

    private Thread hubEventsThread;
    private Thread snapshotThread;

    public static void main(String[] args) {
        SpringApplication.run(AnalyzerApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Starting Analyzer application...");

        hubEventsThread = new Thread(similarityProcessor, "hub-event-processor");
        hubEventsThread.start();
        log.info("Started hub-event-processor thread");

        snapshotThread = new Thread(userActionProcessor, "snapshot-processor");
        snapshotThread.start();
        log.info("Started snapshot-processor thread");

        log.info("Kafka processors started successfully");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Analyzer application...");

        if (hubEventsThread != null && hubEventsThread.isAlive()) {
            log.info("Interrupting hub-events thread");
            hubEventsThread.interrupt();
        }

        if (snapshotThread != null && snapshotThread.isAlive()) {
            log.info("Interrupting snapshot thread");
            snapshotThread.interrupt();
        }
    }
}