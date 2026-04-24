package com.quiz.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quiz.model.PollResponse;
import com.quiz.model.SubmitRequest;
import com.quiz.model.SubmitResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin HTTP client wrapping the two quiz API endpoints.
 *
 * Uses java.net.http.HttpClient (available since Java 11 — no extra dependency needed).
 */
public class QuizApiClient {

    private static final Logger log = LoggerFactory.getLogger(QuizApiClient.class);

    private static final String BASE_URL   = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";
    private static final int    TIMEOUT_S  = 30;

    private final HttpClient   httpClient;
    private final ObjectMapper mapper;
    private final String       regNo;

    public QuizApiClient(String regNo) {
        this.regNo = regNo;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_S))
                .build();
        this.mapper = new ObjectMapper();
    }

    // -----------------------------------------------------------------------
    // GET /quiz/messages?regNo=<regNo>&poll=<pollIndex>
    // -----------------------------------------------------------------------

    /**
     * Fetches one poll page from the validator.
     *
     * @param pollIndex 0–9
     * @return parsed {@link PollResponse}
     */
    public PollResponse fetchPoll(int pollIndex) throws IOException, InterruptedException {
        String url = BASE_URL + "/quiz/messages?regNo=" + regNo + "&poll=" + pollIndex;
        log.info("→ GET {} (poll={})", url, pollIndex);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_S))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("← HTTP {} for poll={} | body: {}", response.statusCode(), pollIndex, response.body());

        if (response.statusCode() != 200) {
            throw new IOException("Unexpected HTTP status " + response.statusCode()
                    + " for poll=" + pollIndex + ": " + response.body());
        }

        return mapper.readValue(response.body(), PollResponse.class);
    }

    // -----------------------------------------------------------------------
    // POST /quiz/submit
    // -----------------------------------------------------------------------

    /**
     * Submits the final leaderboard exactly once.
     *
     * @param submitRequest the leaderboard payload
     * @return server {@link SubmitResponse}
     */
    public SubmitResponse submitLeaderboard(SubmitRequest submitRequest)
            throws IOException, InterruptedException {

        String body = mapper.writeValueAsString(submitRequest);
        log.info("→ POST {}/quiz/submit | body: {}", BASE_URL, body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/quiz/submit"))
                .timeout(Duration.ofSeconds(TIMEOUT_S))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("← HTTP {} from submit | body: {}", response.statusCode(), response.body());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new IOException("Submit failed with HTTP " + response.statusCode()
                    + ": " + response.body());
        }

        return mapper.readValue(response.body(), SubmitResponse.class);
    }
}
