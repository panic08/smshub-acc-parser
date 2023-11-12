package ru.marthastudios.panic.model;

public class CapMonsterCheckTaskResultResponse {
    private int errorId;
    private String status;
    private Solution solution;

    public static class Solution{
        private String gRecaptchaResponse;
        private String text;

        public String getgRecaptchaResponse() {
            return gRecaptchaResponse;
        }

        public void setgRecaptchaResponse(String gRecaptchaResponse) {
            this.gRecaptchaResponse = gRecaptchaResponse;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public int getErrorId() {
        return errorId;
    }

    public void setErrorId(int errorId) {
        this.errorId = errorId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Solution getSolution() {
        return solution;
    }

    public void setSolution(Solution solution) {
        this.solution = solution;
    }
}
