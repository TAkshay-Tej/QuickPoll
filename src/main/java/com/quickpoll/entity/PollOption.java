package com.quickpoll.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "poll_options",
        indexes = @Index(name = "idx_poll_options_poll", columnList = "poll_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Column(nullable = false, length = 255)
    private String text;

    @Column(nullable = false)
    @Builder.Default
    private Long voteCount = 0L;
}