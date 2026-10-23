package com.quickpoll.service;

import com.quickpoll.dto.PollResponse;
import com.quickpoll.entity.Poll;
import com.quickpoll.entity.PollOption;
import com.quickpoll.entity.User;
import com.quickpoll.entity.Vote;
import com.quickpoll.repository.PollRepository;
import com.quickpoll.repository.PollOptionRepository;
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
        Poll poll = pollRepository.findById(pollId)
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

        boolean alreadyVoted = voteRepository.existsByPollIdAndUserId(pollId, user.getId());
        if (alreadyVoted) {
            throw new IllegalArgumentException("User has already voted for this poll");
        }

        Vote vote = Vote.builder()
                .poll(poll)
                .option(option)
                .user(user)
                .build();

        voteRepository.save(vote);
        option.setVoteCount(option.getVoteCount() + 1);
        pollOptionRepository.save(option);

        return pollService.getPollById(pollId, username);
    }
}
