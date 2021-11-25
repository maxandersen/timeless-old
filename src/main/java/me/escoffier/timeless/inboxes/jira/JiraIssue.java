package me.escoffier.timeless.inboxes.jira;

import me.escoffier.timeless.helpers.ProjectHints;
import me.escoffier.timeless.model.NewTaskRequest;

import static me.escoffier.timeless.helpers.DueDates.todayOrTomorrow;

public class JiraIssue {

    public String html_url;
    public String title;
    public String project;
    public String key;
    public boolean open;
    public String reporter;
    public String assignee;

    public String url() {
        return html_url;
    }

    public String title() {
        return title;
    }

    public String project() {
        return project;
    }

    public boolean isOpen() {
        return open;
    }

    public NewTaskRequest asNewTaskRequest(String content, String project, String label) {
        NewTaskRequest request = new NewTaskRequest(
                content,
                html_url,
                project,
                todayOrTomorrow()
        );
        request.addLabels(label,"timeless/jira");
        return request;
    }
}
