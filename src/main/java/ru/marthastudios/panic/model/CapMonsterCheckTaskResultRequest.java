package ru.marthastudios.panic.model;

public class CapMonsterCheckTaskResultRequest {
    public CapMonsterCheckTaskResultRequest(String clientKey, long taskId) {
        this.clientKey = clientKey;
        this.taskId = taskId;
    }

    private String clientKey;
    private long taskId;
}
