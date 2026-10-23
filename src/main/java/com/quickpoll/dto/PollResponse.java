package com.quickpoll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollResponse {

    private Long id;
    private String question;
    private String createdBy;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean expired;
    private List<PollOptionResponse> options;
    private Long totalVotes;
}
