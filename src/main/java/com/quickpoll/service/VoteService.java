package com.quickpoll.service;

import com.quickpoll.dto.PollResponse;
import com.quickpoll.entity.Poll;
import com.quickpoll.entity.PollOption;
import com.quickpoll.entity.User;
import com.quickpoll.entity.Vote;
import com.quickpoll.repository.PollOptionRepository;
import com.quickpoll.repository.PollRepository;
import com.quickpoll.repository.UserRepository;
import com.quickpoll.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final UserRepository userRepository;
    private final PollService pollService;

    @Transactional
    public PollResponse castVote(Long pollId, Long optionId, String username) {
        // 1. Take a row-level lock (SELECT ... FOR UPDATE) on the poll.
        //    Concurrent votes on the same poll queue up here until this
        //    transaction commits; votes on other polls are not blocked.
        Poll poll = pollRepository.findByIdForUpdate(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found"));

        if (poll.isExpired()) {
            throw new IllegalArgumentException("Poll has expired");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        PollOption option = pollOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Poll option not found"));

        if (!option.getPoll().getId().equals(pollId)) {
            throw new IllegalArgumentException("Option does not belong to this poll");
        }

        // 2. Safe from races now: we hold the poll lock. The unique index on
        //    (poll_id, user_id) remains as a second line of defence.
        if (voteRepository.existsByPollIdAndUserId(pollId, user.getId())) {
            throw new IllegalArgumentException("User has already voted for this poll");
        }

        voteRepository.save(Vote.builder()
                .poll(poll)
                .option(option)
                .user(user)
                .build());

        // 3. Atomic increment in SQL (vote_count = vote_count + 1),
        //    no read-modify-write in Java.
        pollOptionRepository.incrementVoteCount(optionId);

        return pollService.getPollById(pollId, username);
    }
}
