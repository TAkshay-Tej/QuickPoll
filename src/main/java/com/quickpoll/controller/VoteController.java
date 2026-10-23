package com.quickpoll.controller;

import com.quickpoll.dto.PollResponse;
import com.quickpoll.dto.VoteRequest;
import com.quickpoll.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping("/{pollId}/vote")
    public ResponseEntity<PollResponse> vote(@PathVariable Long pollId,
                                             @Valid @RequestBody VoteRequest request,
                                             Authentication auth) {
        return ResponseEntity.ok(voteService.castVote(pollId, request.getOptionId(), auth.getName()));
    }
}