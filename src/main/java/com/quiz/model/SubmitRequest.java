package com.quiz.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Payload sent to POST /quiz/submit */
public class SubmitRequest {

    private String regNo;
    private List<LeaderboardEntry> leaderboard;

    public SubmitRequest() {}

    public SubmitRequest(String regNo, List<LeaderboardEntry> leaderboard) {
        this.regNo       = regNo;
        this.leaderboard = leaderboard;
    }

    public String                 getRegNo()       { return regNo; }
    public List<LeaderboardEntry> getLeaderboard() { return leaderboard; }

    public void setRegNo(String regNo)                          { this.regNo = regNo; }
    public void setLeaderboard(List<LeaderboardEntry> leaderboard) { this.leaderboard = leaderboard; }
}
