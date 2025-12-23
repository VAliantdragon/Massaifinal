package com.shadowledger.event_service.entity;


import com.shadowledger.event_service.dto.EventDto;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "received_events")
public class ReceivedEvent {

    @Id
    private String eventId;

    private String accountId;
    private String type;
    private BigDecimal amount;
    private long timestamp;

    protected ReceivedEvent() {}

    public ReceivedEvent(EventDto dto) {
        this.eventId = dto.eventId();
        this.accountId = dto.accountId();
        this.type = dto.type();
        this.amount = dto.amount();
        this.timestamp = dto.timestamp();
    }
}