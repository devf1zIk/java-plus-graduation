package ru.practicum.gateway;

@SpringBootAplication
@EnableDiscoveryClient
public class Gateway {
    public static void main(String[] args) {
        SpringApplication.run(Gateway.class, args);
    }
}
