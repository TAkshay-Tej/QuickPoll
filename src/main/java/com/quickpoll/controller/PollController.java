package com.quickpoll.controller;

import com.quickpoll.dto.PollCreateRequest;
import com.quickpoll.dto.PollResponse;
import com.quickpoll.service.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @PostMapping
    public ResponseEntity<PollResponse> createPoll(@Valid @RequestBody PollCreateRequest request,
                                                   Authentication auth) {
        return ResponseEntity.ok(pollService.createPoll(request, auth.getName()));
    }

    @GetMapping
    public ResponseEntity<Page<PollResponse>> getPolls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Authentication auth) {
        return ResponseEntity.ok(pollService.getPolls(page, size, search, auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PollResponse> getPoll(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(pollService.getPollById(id, auth.getName()));
    }
}