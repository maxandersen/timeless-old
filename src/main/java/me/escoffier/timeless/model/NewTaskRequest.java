package me.escoffier.timeless.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NewTaskRequest {

    public final String content;
    public final String project;
    public final String due;
    public final List<String> labels = new ArrayList<>();
    public int priority = -1;
    public final String description;

    public NewTaskRequest(String content, String project, String due) {
        this.content = content;
        this.project = project;
        this.due = due;
        this.description = "";
    }

    public NewTaskRequest(String content, String link, String project, String due) {
        this.content = String.format("[%s](%s)", content, link);
        this.project = project;
        this.due = due;
        this.description = "Source: " + link;
    }

    public void addLabels(String... labels) {
        this.labels.addAll(Arrays.asList(labels));
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getIdentifier() {
        return content;
    }
}
