package ru.marthastudios.panic.model;

public class CapMonsterCreateTaskRequest {
    public CapMonsterCreateTaskRequest(String clientKey, Task task) {
        this.clientKey = clientKey;
        this.task = task;
    }

    private String clientKey;
    private Task task;

    public static class Task{
        public Task(String type, String websiteURL, String websiteKey) {
            this.type = type;
            this.websiteURL = websiteURL;
            this.websiteKey = websiteKey;
        }

        private String type;
        private String websiteURL;
        private String websiteKey;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getWebsiteURL() {
            return websiteURL;
        }

        public void setWebsiteURL(String websiteURL) {
            this.websiteURL = websiteURL;
        }

        public String getWebsiteKey() {
            return websiteKey;
        }

        public void setWebsiteKey(String websiteKey) {
            this.websiteKey = websiteKey;
        }
    }
    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }
}
