package com.quiz.service;

import com.quiz.model.LeaderboardEntry;
import com.quiz.model.QuizEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Stateful service that:
 *  1. Accepts raw {@link QuizEvent}s from each poll.
 *  2. Deduplicates events using the composite key  (roundId :: participant).
 *  3. Aggregates per-participant total scores.
 *  4. Produces a sorted leaderboard and a grand total.
 *
 * <p>Thread-safety is not required here (single-threaded polling), but the
 * internal collections are kept private to avoid accidental mutation.</p>
 */
public class LeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);

    /**
     * Tracks which (roundId + participant) keys we have already counted.
     * Key   → deduplicationKey()  e.g. "R1::Alice"
     * Value → the score that was recorded for that key (informational)
     */
    private final Map<String, Integer> seenEvents = new HashMap<>();

    /**
     * Running total per participant (after deduplication).
     */
    private final Map<String, Integer> scores = new HashMap<>();

    /** Count of events accepted vs. rejected (for logging). */
    private int accepted = 0;
    private int rejected = 0;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Process all events from a single poll response.
     * Duplicates (same dedup key seen before) are silently dropped.
     *
     * @param events list of events from one poll
     * @param pollIndex poll index — used only for logging
     */
    public void processEvents(List<QuizEvent> events, int pollIndex) {
        if (events == null || events.isEmpty()) {
            log.warn("Poll {} returned no events.", pollIndex);
            return;
        }

        for (QuizEvent event : events) {
            String key = event.getRoundId().trim() + "::" + event.getParticipant().trim();

            if (seenEvents.containsKey(key)) {
                log.debug("Poll {} — DUPLICATE ignored: {} (score={})", pollIndex, key, event.getScore());
                rejected++;
            } else {
                seenEvents.put(key, event.getScore());
                scores.merge(event.getParticipant(), event.getScore(), Integer::sum);
                log.debug("Poll {} — ACCEPTED: {} score={}", pollIndex, key, event.getScore());
                accepted++;
            }
        }
    }

    /**
     * Builds the final leaderboard sorted by totalScore descending.
     *
     * @return immutable sorted list of {@link LeaderboardEntry}
     */
    public List<LeaderboardEntry> buildLeaderboard() {
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            leaderboard.add(new LeaderboardEntry(entry.getKey(), entry.getValue()));
        }
        Collections.sort(leaderboard);          // uses compareTo → descending score
        return leaderboard;
    }

    /**
     * Grand total of all participants' deduplicated scores.
     */
    public int computeGrandTotal() {
        return scores.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Prints a summary table to the log.
     */
    public void logSummary() {
        List<LeaderboardEntry> board = buildLeaderboard();
        log.info("=======================================================");
        log.info("LEADERBOARD  (events accepted={}, duplicates rejected={})", accepted, rejected);
        log.info("=======================================================");
        log.info("{:<20} {:>12}", "Participant", "Total Score");
        log.info("-------------------------------------------------------");
        board.forEach(e -> log.info("{:<20} {:>12}", e.getParticipant(), e.getTotalScore()));
        log.info("-------------------------------------------------------");
        log.info("{:<20} {:>12}", "GRAND TOTAL", computeGrandTotal());
        log.info("=======================================================");
    }
}
