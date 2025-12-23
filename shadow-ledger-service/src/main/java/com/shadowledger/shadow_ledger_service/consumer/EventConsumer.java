package com.shadowledger.shadow_ledger_service.consumer;

import com.shadowledger.shadow_ledger_service.dto.EventDto;
import com.shadowledger.shadow_ledger_service.model.EventType;
import com.shadowledger.shadow_ledger_service.model.LedgerEvent;
import com.shadowledger.shadow_ledger_service.repository.LedgerEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    private final LedgerEventRepository repository;

    public EventConsumer(LedgerEventRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(
            topics = {"transactions.raw", "transactions.corrections"},
            groupId = "shadow-ledger-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(EventDto dto) {
        if (dto == null) return;

        // Deduplication - skip if eventId already exists
        if (repository.findByEventId(dto.eventId()).isPresent()) {
            System.out.println("Skipping duplicate event: " + dto.eventId());
            return;
        }

        LedgerEvent event = new LedgerEvent();
        event.setEventId(dto.eventId());
        event.setAccountId(dto.accountId());

        try {
            event.setType(EventType.valueOf(dto.type().toUpperCase()));
        } catch (IllegalArgumentException | NullPointerException e) {
            System.err.println("Invalid event type: " + dto.type());
            return;
        }

        event.setAmount(dto.amount().doubleValue());
        event.setTimestamp(dto.timestamp());

        repository.save(event);
        System.out.println("Consumed and saved event: " + dto.eventId() + " for account: " + dto.accountId());
    }
}

