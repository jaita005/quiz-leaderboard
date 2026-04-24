package com.quiz.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Response body from POST /quiz/submit */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmitResponse {

    private boolean isCorrect;
    private boolean isIdempotent;
    private int     submittedTotal;
    private int     expectedTotal;
    private String  message;

    public SubmitResponse() {}

    public boolean isCorrect()       { return isCorrect; }
    public boolean isIdempotent()    { return isIdempotent; }
    public int     getSubmittedTotal() { return submittedTotal; }
    public int     getExpectedTotal()  { return expectedTotal; }
    public String  getMessage()      { return message; }

    public void setCorrect(boolean correct)           { isCorrect = correct; }
    public void setIdempotent(boolean idempotent)     { isIdempotent = idempotent; }
    public void setSubmittedTotal(int submittedTotal) { this.submittedTotal = submittedTotal; }
    public void setExpectedTotal(int expectedTotal)   { this.expectedTotal = expectedTotal; }
    public void setMessage(String message)            { this.message = message; }

    @Override
    public String toString() {
        return String.format(
            "SubmitResponse{isCorrect=%b, isIdempotent=%b, submittedTotal=%d, expectedTotal=%d, message='%s'}",
            isCorrect, isIdempotent, submittedTotal, expectedTotal, message);
    }
}
