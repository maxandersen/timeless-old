package me.escoffier.timeless.inboxes.github;

import me.escoffier.timeless.helpers.ProjectHints;
import me.escoffier.timeless.model.NewTaskRequest;

import static me.escoffier.timeless.helpers.DueDates.todayOrTomorrow;

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

    public NewTaskRequest asNewTaskRequest(ProjectHints hints) {
        String content = getTaskName();
        NewTaskRequest request = new NewTaskRequest(
                content,
                html_url,
                hints.lookup(html_url),
                todayOrTomorrow()
        );
        request.addLabels("Devel","timeless/github");
        return request;
    }

    public String getTaskName() {
        return "Fix " + title + " " + project() + "#" + number;
    }

    public static class Repository {
        public String full_name;
    }
}
