package com.quickpoll.repository;

import com.quickpoll.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    List<PollOption> findByPollId(Long pollId);

    // Atomic counter update. clearAutomatically makes the following read
    // (pollService.getPollById) see the fresh count instead of a stale cached entity.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PollOption o set o.voteCount = o.voteCount + 1 where o.id = :id")
    int incrementVoteCount(@Param("id") Long id);
}