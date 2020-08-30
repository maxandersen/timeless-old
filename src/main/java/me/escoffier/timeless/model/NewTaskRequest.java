package me.escoffier.timeless.model;

public class NewTaskRequest {

    public final String content;
    public final String project;
    public final String due;

    public NewTaskRequest(String content, String project, String due) {
        this.content = content;
        this.project = project;
        this.due = due;
    }

    public NewTaskRequest(String content, String link, String project, String due) {
        this.content = String.format("[%s](%s)", content, link);
        this.project = project;
        this.due = due;
    }

    public String getIdentifier() {
        return content;
    }
}
