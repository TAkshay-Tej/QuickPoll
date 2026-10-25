package com.quickpoll;

import com.quickpoll.dto.PollCreateRequest;
import com.quickpoll.dto.PollOptionResponse;
import com.quickpoll.dto.PollResponse;
import com.quickpoll.entity.PollOption;
import com.quickpoll.entity.User;
import com.quickpoll.repository.PollOptionRepository;
import com.quickpoll.repository.UserRepository;
import com.quickpoll.service.PollService;
import com.quickpoll.service.VoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Concurrency tests. Requires PostgreSQL running: `docker compose up -d`
 * Run: ./mvnw test -Dtest=VoteConcurrencyTest
 */
@SpringBootTest
class VoteConcurrencyTest {

    private static final int TOTAL_VOTES = 500;
    private static final int THREADS = 50;
    private static final int OPTIONS = 3;

    @Autowired private VoteService voteService;
    @Autowired private PollService pollService;
    @Autowired private UserRepository userRepository;
    @Autowired private PollOptionRepository pollOptionRepository;

    private User newUser(String prefix) {
        return userRepository.save(User.builder()
                .username(prefix + "_" + UUID.randomUUID().toString().substring(0, 8))
                .password("not-used")
                .build());
    }

    private PollResponse newPoll(String creator) {
        return pollService.createPoll(PollCreateRequest.builder()
                .question("Concurrency test " + UUID.randomUUID())
                .options(List.of("A", "B", "C"))
                .build(), creator);
    }

    @Test
    void manyDistinctUsersVotingAtOnce_noLostVotes() throws Exception {
        User creator = newUser("creator");
        PollResponse poll = newPoll(creator.getUsername());
        List<Long> optionIds = poll.getOptions().stream().map(PollOptionResponse::getId).toList();

        List<String> voters = IntStream.range(0, TOTAL_VOTES)
                .mapToObj(i -> newUser("voter").getUsername())
                .toList();

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Long> latenciesMs = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger failures = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < TOTAL_VOTES; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                try {
                    startGate.await();
                    long t0 = System.nanoTime();
                    voteService.castVote(poll.getId(), optionIds.get(idx % OPTIONS), voters.get(idx));
                    latenciesMs.add((System.nanoTime() - t0) / 1_000_000);
                } catch (Exception e) {
                    failures.incrementAndGet();
                    e.printStackTrace();
                }
            }));
        }

        long start = System.nanoTime();
        startGate.countDown();
        for (Future<?> f : futures) f.get(120, TimeUnit.SECONDS);
        double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
        pool.shutdown();

        // ---- correctness ----
        assertEquals(0, failures.get(), "No vote should fail");

        List<PollOption> options = pollOptionRepository.findByPollId(poll.getId());
        long total = options.stream().mapToLong(PollOption::getVoteCount).sum();
        assertEquals(TOTAL_VOTES, total, "Every vote must be counted exactly once");

        for (PollOption o : options) {
            int idx = optionIds.indexOf(o.getId());
            long expected = IntStream.range(0, TOTAL_VOTES).filter(i -> i % OPTIONS == idx).count();
            assertEquals(expected, o.getVoteCount(), "Per-option count mismatch");
        }

        // ---- measured numbers for your resume ----
        List<Long> sorted = new ArrayList<>(latenciesMs);
        Collections.sort(sorted);
        System.out.printf(
                "%n=== QuickPoll vote benchmark ===%nvotes=%d threads=%d time=%.2fs throughput=%.0f votes/sec%n"
                        + "latency p50=%dms p95=%dms max=%dms%n",
                TOTAL_VOTES, THREADS, seconds, TOTAL_VOTES / seconds,
                sorted.get(sorted.size() / 2),
                sorted.get((int) (sorted.size() * 0.95)),
                sorted.get(sorted.size() - 1));
    }

    @Test
    void sameUserVotingConcurrently_onlyOneVoteAccepted() throws Exception {
        User creator = newUser("creator");
        PollResponse poll = newPoll(creator.getUsername());
        Long optionId = poll.getOptions().get(0).getId();
        String voter = newUser("dupvoter").getUsername();

        int attempts = 20;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            futures.add(pool.submit(() -> {
                try {
                    startGate.await();
                    voteService.castVote(poll.getId(), optionId, voter);
                    success.incrementAndGet();
                } catch (Exception ignored) {
                    // expected for all but one attempt
                }
            }));
        }

        startGate.countDown();
        for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, success.get(), "Exactly one vote from the same user must succeed");
        long total = pollOptionRepository.findByPollId(poll.getId()).stream()
                .mapToLong(PollOption::getVoteCount).sum();
        assertEquals(1, total, "Only one vote should be counted");
    }
}
