package com.quickpoll.repository;

import com.quickpoll.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByPollIdAndUserId(Long pollId, Long userId);
    Optional<Vote> findByPollIdAndUserId(Long pollId, Long userId);
}