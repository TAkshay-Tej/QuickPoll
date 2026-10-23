package com.quickpoll.repository;

import com.quickpoll.entity.Poll;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {

    // Used for the Home screen list (paginated, newest first)
    Page<Poll> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Simple search by question text (case-insensitive)
    Page<Poll> findByQuestionContainingIgnoreCase(String keyword, Pageable pageable);

    // Row-level lock so two simultaneous votes on the same poll
    // can't both read a stale option list at once. Combined with
    // the Vote table's unique constraint, this is our second line
    // of defense against race conditions during voting.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Poll p where p.id = :id")
    Optional<Poll> findByIdForUpdate(@Param("id") Long id);
}