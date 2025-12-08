package ru.yandex.practicum.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.HitDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsClient extends BaseClient {

    private static final String SERVICE_ID = "STATS-SERVER";
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;

    @Autowired
    public StatsClient(RestTemplateBuilder builder, DiscoveryClient discoveryClient) {
        super(builder
                .setConnectTimeout(java.time.Duration.ofSeconds(5))
                .setReadTimeout(java.time.Duration.ofSeconds(10))
                .build());

        this.discoveryClient = discoveryClient;

        RetryTemplate template = new RetryTemplate();
        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(3000L);
        template.setBackOffPolicy(backOff);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy(3);
        template.setRetryPolicy(retryPolicy);

        this.retryTemplate = template;
    }

    private String getBaseUrl() {
        ServiceInstance instance = retryTemplate.execute(ctx -> {
            List<ServiceInstance> instances = discoveryClient.getInstances(SERVICE_ID);
            if (instances == null || instances.isEmpty()) {
                throw new RuntimeException("Stats-service не найден");
            }
            return instances.get(0);
        });
        return "http://" + instance.getHost() + ":" + instance.getPort();
    }

    public ResponseEntity<Object> getStats(String start, String end, List<String> uris, boolean unique) {
        String baseUrl = getBaseUrl();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("start", start);
        parameters.put("end", end);
        parameters.put("unique", unique);

        String path = "/stats?start={start}&end={end}&unique={unique}";

        if (uris != null && !uris.isEmpty()) {
            parameters.put("uris", String.join(",", uris));
            path += "&uris={uris}";
        }
        return get(baseUrl + path, parameters);
    }

    public ResponseEntity<Object> create(HitDto hitDto) {
        String baseUrl = getBaseUrl();
        return post(baseUrl + "/hit", hitDto);
    }
}