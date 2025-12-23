package com.shadowledger.event_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class KafkaConfig {
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            org.springframework.kafka.core.ProducerFactory<String, Object> pf) {
        return new KafkaTemplate<>(pf);
    }
}