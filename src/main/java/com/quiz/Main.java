package com.quiz;

import com.quiz.client.QuizApiClient;
import com.quiz.model.LeaderboardEntry;
import com.quiz.model.PollResponse;
import com.quiz.model.SubmitRequest;
import com.quiz.model.SubmitResponse;
import com.quiz.service.LeaderboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Entry point for the Quiz Leaderboard System.
 *
 * <p>Orchestration flow:
 * <ol>
 *   <li>Poll the /quiz/messages endpoint 10 times (poll=0..9).</li>
 *   <li>Wait 5 s between each poll (mandatory per assignment spec).</li>
 *   <li>Feed each response into {@link LeaderboardService} which deduplicates
 *       on (roundId + participant) and accumulates per-participant scores.</li>
 *   <li>Build a sorted leaderboard and log it.</li>
 *   <li>POST /quiz/submit exactly once with the final leaderboard.</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>
 *   java -jar quiz-leaderboard.jar &lt;regNo&gt;
 * </pre>
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    /** Total poll rounds mandated by the assignment. */
    private static final int TOTAL_POLLS = 10;

    /** Mandatory inter-poll delay in milliseconds (5 seconds). */
    private static final long POLL_DELAY_MS = 5_000L;

    public static void main(String[] args) {
        // ----------------------------------------------------------------
        // 0. Read registration number from CLI or fall back to default
        // ----------------------------------------------------------------
        String regNo = (args.length > 0) ? args[0].trim() : "2024CS101";
        log.info("Starting Quiz Leaderboard System | regNo={}", regNo);

        QuizApiClient    client  = new QuizApiClient(regNo);
        LeaderboardService service = new LeaderboardService();

        // ----------------------------------------------------------------
        // 1–2. Poll 10 times with 5-second gaps
        // ----------------------------------------------------------------
        for (int pollIndex = 0; pollIndex < TOTAL_POLLS; pollIndex++) {
            try {
                log.info("──────────────────────────────────────────────────────");
                log.info("Fetching poll {}/{}", pollIndex, TOTAL_POLLS - 1);

                PollResponse response = client.fetchPoll(pollIndex);

                // 3. Feed events into service (deduplication happens here)
                service.processEvents(response.getEvents(), pollIndex);

                log.info("Poll {} complete. Events in payload: {}",
                        pollIndex,
                        response.getEvents() == null ? 0 : response.getEvents().size());

            } catch (Exception e) {
                log.error("Error during poll {}: {}", pollIndex, e.getMessage(), e);
                // Fail fast — a missing poll means potentially missing data
                System.exit(1);
            }

            // Mandatory 5-second delay between polls (skip after the last one)
            if (pollIndex < TOTAL_POLLS - 1) {
                log.info("Waiting {} ms before next poll …", POLL_DELAY_MS);
                try {
                    Thread.sleep(POLL_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted during poll delay", ie);
                    System.exit(1);
                }
            }
        }

        // ----------------------------------------------------------------
        // 4–6. Build leaderboard & compute grand total
        // ----------------------------------------------------------------
        service.logSummary();

        List<LeaderboardEntry> leaderboard = service.buildLeaderboard();
        int grandTotal = service.computeGrandTotal();

        log.info("Grand total across all participants: {}", grandTotal);

        // ----------------------------------------------------------------
        // 7. Submit leaderboard exactly once
        // ----------------------------------------------------------------
        try {
            SubmitRequest  submitRequest  = new SubmitRequest(regNo, leaderboard);
            SubmitResponse submitResponse = client.submitLeaderboard(submitRequest);

            log.info("══════════════════════════════════════════════════════");
            log.info("SUBMISSION RESULT");
            log.info("══════════════════════════════════════════════════════");
            log.info("  isCorrect      : {}", submitResponse.isCorrect());
            log.info("  isIdempotent   : {}", submitResponse.isIdempotent());
            log.info("  submittedTotal : {}", submitResponse.getSubmittedTotal());
            log.info("  expectedTotal  : {}", submitResponse.getExpectedTotal());
            log.info("  message        : {}", submitResponse.getMessage());
            log.info("══════════════════════════════════════════════════════");

            if (!submitResponse.isCorrect()) {
                log.error("Submission was NOT correct. Please review the deduplication logic.");
                System.exit(2);
            }

            log.info("✓ Leaderboard submitted successfully!");

        } catch (Exception e) {
            log.error("Failed to submit leaderboard: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
