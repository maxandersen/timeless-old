package me.escoffier.timeless.inboxes.github;

import me.escoffier.timeless.model.NewTaskRequest;

public class Issue {

    public String html_url;
    public String title;
    public Repository repository;
    public int number;
    public String state;

    public String url() {
        return html_url;
    }

    public String title() {
        return title;
    }

    public String project() {
        return repository.full_name;
    }

    public boolean isOpen() {
        return "open".equalsIgnoreCase(state);
    }

    public NewTaskRequest asNewTaskRequest() {
        String content = getTaskName();
        return new NewTaskRequest(
                content,
                html_url,
                null,
                null
        );
    }

    public String getTaskName() {
        return "Fix " + title + " " + project() + "#" + number;
    }

    public static class Repository {
        public String full_name;
    }
}
