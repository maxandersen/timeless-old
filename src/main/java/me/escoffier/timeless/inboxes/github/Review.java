package me.escoffier.timeless.inboxes.github;

import me.escoffier.timeless.helpers.ProjectHints;
import me.escoffier.timeless.model.NewTaskRequest;

import static me.escoffier.timeless.helpers.DueDates.todayOrTomorrow;

public class Review {

    public String html_url;
    public String title;
    public String repository;
    public int number;

    public Review(String title, String repository, int number, String html_url) {
        this.html_url = html_url;
        this.title = title;
        this.repository = repository;
        this.number = number;
    }

    public String url() {
        return html_url;
    }

    public String title() {
        return title;
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
        return "Review PR " + title + " " + repository + "#" + number;
    }
}
