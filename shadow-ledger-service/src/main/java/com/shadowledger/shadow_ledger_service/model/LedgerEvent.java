package com.shadowledger.shadow_ledger_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ledger_events", uniqueConstraints = @UniqueConstraint(columnNames = "eventId"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventId;
    private String accountId;

    @Enumerated(EnumType.STRING)
    private EventType type;

    private Double amount;
    private Long timestamp;
}

