package com.quickpoll.service;

import com.quickpoll.dto.PollCreateRequest;
import com.quickpoll.dto.PollOptionResponse;
import com.quickpoll.dto.PollResponse;
import com.quickpoll.entity.Poll;
import com.quickpoll.entity.PollOption;
import com.quickpoll.entity.User;
import com.quickpoll.repository.PollRepository;
import com.quickpoll.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final UserRepository userRepository;

    @Transactional
    public PollResponse createPoll(PollCreateRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Poll poll = Poll.builder()
                .question(request.getQuestion())
                .createdBy(user)
                .expiresAt(request.getExpiresAt())
                .build();

        request.getOptions().forEach(optionText -> {
            PollOption option = PollOption.builder()
                    .text(optionText)
                    .poll(poll)
                    .voteCount(0L)
                    .build();
            poll.getOptions().add(option);
        });

        Poll savedPoll = pollRepository.save(poll);
        return toPollResponse(savedPoll);
    }

    public Page<PollResponse> getPolls(int page, int size, String search, String username) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Poll> polls;

        if (search != null && !search.isEmpty()) {
            polls = pollRepository.findByQuestionContainingIgnoreCase(search, pageable);
        } else {
            polls = pollRepository.findAll(pageable);
        }

        return polls.map(this::toPollResponse);
    }

    public PollResponse getPollById(Long id, String username) {
        Poll poll = pollRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found"));

        return toPollResponse(poll);
    }

    private PollResponse toPollResponse(Poll poll) {
        long totalVotes = poll.getOptions().stream()
                .mapToLong(PollOption::getVoteCount)
                .sum();

        return PollResponse.builder()
                .id(poll.getId())
                .question(poll.getQuestion())
                .createdBy(poll.getCreatedBy().getUsername())
                .createdAt(poll.getCreatedAt())
                .expiresAt(poll.getExpiresAt())
                .expired(poll.isExpired())
                .totalVotes(totalVotes)
                .options(poll.getOptions().stream()
                        .map(opt -> PollOptionResponse.builder()
                                .id(opt.getId())
                                .text(opt.getText())
                                .voteCount(opt.getVoteCount())
                                .percentage(totalVotes > 0 ? (double) opt.getVoteCount() / totalVotes * 100 : 0.0)
                                .build())
                        .toList())
                .build();
    }
}
