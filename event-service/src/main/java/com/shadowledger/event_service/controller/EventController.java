package com.shadowledger.event_service.controller;
import com.shadowledger.event_service.dto.EventDto;
import com.shadowledger.event_service.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody EventDto dto) {
        service.process(dto);
        return ResponseEntity.accepted().build();
    }
}