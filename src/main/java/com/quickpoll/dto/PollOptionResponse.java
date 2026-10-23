package com.quickpoll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOptionResponse {

    private Long id;
    private String text;
    private Long voteCount;
    private Double percentage;
}
