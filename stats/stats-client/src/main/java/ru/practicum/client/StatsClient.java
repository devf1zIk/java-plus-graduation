package ru.practicum.client;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.dto.HitDto;
import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class StatsClient extends BaseClient {

    private static final String STATS_SERVICE_ID = "stats-server";
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;

    public StatsClient(RestTemplateBuilder builder, DiscoveryClient discoveryClient) {
        super(builder.build());
        this.discoveryClient = discoveryClient;

        this.retryTemplate = new RetryTemplate();

        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(backOff);

        MaxAttemptsRetryPolicy policy = new MaxAttemptsRetryPolicy();
        policy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(policy);
    }

    private ServiceInstance getInstance() {
        return discoveryClient.getInstances(STATS_SERVICE_ID)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Сервис статистики '" + STATS_SERVICE_ID + "' не найден в Eureka"
                ));
    }

    public ResponseEntity<Object> getStats(String start, String end, List<String> uris, boolean unique) {
        URI uri = makeUri("/stats?start={start}&end={end}&uris={uris}&unique={unique}");
        Map<String, Object> params = Map.of(
                "start", start,
                "end", end,
                "uris", uris,
                "unique", unique
        );
        return get(uri.toString(), params);
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(ctx -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    public ResponseEntity<Object> create(HitDto hitDto) {
        URI uri = makeUri("/hit");
        return post(uri.toString(), hitDto);
    }
}

