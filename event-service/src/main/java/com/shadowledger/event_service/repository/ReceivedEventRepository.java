package com.shadowledger.event_service.repository;


import com.shadowledger.event_service.entity.ReceivedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivedEventRepository
        extends JpaRepository<ReceivedEvent, String> {
}