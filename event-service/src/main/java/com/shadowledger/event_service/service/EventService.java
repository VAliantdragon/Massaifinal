package com.shadowledger.event_service.service;

import com.shadowledger.event_service.dto.EventDto;
import com.shadowledger.event_service.entity.ReceivedEvent;
import com.shadowledger.event_service.repository.ReceivedEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EventService {

    private final ReceivedEventRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventService(ReceivedEventRepository repository,
                        KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void process(EventDto dto) {
        validate(dto);

        if (repository.existsById(dto.eventId())) {
            throw new IllegalStateException("Duplicate eventId");
        }

        repository.save(new ReceivedEvent(dto));
        kafkaTemplate.send("transactions.raw", dto.eventId(), dto);
    }

    private void validate(EventDto dto) {
        if (dto.amount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be positive");

        if (!dto.type().equals("credit") && !dto.type().equals("debit"))
            throw new IllegalArgumentException("Invalid type");
    }
}