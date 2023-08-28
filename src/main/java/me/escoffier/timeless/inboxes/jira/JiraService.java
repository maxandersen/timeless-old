package me.escoffier.timeless.inboxes.jira;

import com.atlassian.httpclient.api.Request;
import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import io.quarkus.arc.log.LoggerName;
import jakarta.annotation.PostConstruct;
import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.Inbox;
import me.escoffier.timeless.model.NewTaskRequest;
import me.escoffier.timeless.model.Task;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class JiraService implements Inbox {

    @LoggerName("Jira")
    Logger LOGGER;

    @ConfigProperty(name = "jira.url", defaultValue = "https://issues.redhat.com")
    String jiraURL;
    @ConfigProperty(name = "jira.user")
    String jiraUser;
    @ConfigProperty(name = "jira.password")
    Optional<String> jiraPassword;
    @ConfigProperty(name = "jira.token")
    Optional<String> jiraToken;
    @ConfigProperty(name = "jira.label")
    String defaultLabel;
    @ConfigProperty(name = "jira.jql", defaultValue = "project = QUARKUS AND (assignee = currentUser() OR reporter = currentUser()) and resolution = Unresolved")
    String jiraQuery;

    @ConfigProperty(name = "jira.projects")
    Map<String,String> projectHints;

    String myself() {
        return jiraUser;
    }


    private final List<JiraIssue> issues = new ArrayList<>();

    @PostConstruct
    public void fetch() throws URISyntaxException {
        issues.clear();
        URI jirauri = new URI(jiraURL);
        JiraRestClient jira;

        if(jiraPassword.isPresent()) {
            LOGGER.infof("\uD83D\uDEB6 Connecting to %s with %s using a password", jirauri, jiraUser);

            jira = new AsynchronousJiraRestClientFactory()
                    .createWithBasicHttpAuthentication(jirauri, jiraUser, jiraPassword.get());
        } else if (jiraToken.isPresent()) {
            LOGGER.infof("\uD83D\uDEB6 Connecting to %s with %s using a token", jirauri, jiraUser);
            jira = new AsynchronousJiraRestClientFactory().createWithAuthenticationHandler(jirauri, new BearerHttpAuthenticationHandler(jiraToken.get()));
        } else {
            throw new IllegalStateException("Neither jira.password nor jira.token set");
        }

        LOGGER.infof("\uD83D\uDEB6 Retrieving Jira issues %s", jiraQuery);
        SearchResult searchResultsAll = jira.getSearchClient().searchJql(jiraQuery).claim();
        searchResultsAll.getIssues().forEach(issue -> {
            JiraIssue ji = toIssue(issue);
            if (relevant(ji)) {
                issues.add(ji);
            } else {
                LOGGER.debugf("Ignoring non-relevant jira issue %s", ji.html_url);
            }
        });
        LOGGER.infof("\uD83D\uDEB6 %d jira issues", issues.size());
    }

    public static class BearerHttpAuthenticationHandler implements AuthenticationHandler {

        private static final String AUTHORIZATION_HEADER = "Authorization";
        private final String token;

        public BearerHttpAuthenticationHandler(final String token) {
            this.token = token;
        }

        @Override
        public void configure(Request.Builder builder) {
            builder.setHeader(AUTHORIZATION_HEADER, "Bearer " + token);
        }
    }

    private boolean relevant(JiraIssue issue) {
        return myself().equals(issue.reporter) || myself().equals(issue.assignee);
    }

    private JiraIssue toIssue(Issue issue) {
        JiraIssue jira = new JiraIssue();
        jira.project = issue.getProject().getKey();
        jira.html_url = jiraURL + "/browse/" + issue.getKey();
        jira.key = issue.getKey();
        jira.title = issue.getSummary();
        jira.reporter = issue.getReporter().getName();
        if(issue.getAssignee()!=null) {
            jira.assignee = issue.getAssignee().getName();
        }

        jira.open = issue.getResolution()==null;

        return jira;
    }

    @Override
    public List<Runnable> getPlan(Backend backend) {
        List<Task> existingIssues = backend.getMatchingTasks(this::isAJiraIssue);

        // 1 - If fetched contains an open issue without an associated task -> create new task
        // 2 - If fetched contains an open issue with an uncompleted associated task -> do nothing
        // 3 - If fetched contains a closed issue with an uncompleted associated task -> complete the task
        // 4 - If backend contains issue tasks (existingIssues) without an associated issue in fetched -> complete task

        List<Runnable> actions = new ArrayList<>();
        for (JiraIssue issue : issues) {
            NewTaskRequest request = issue.asNewTaskRequest(getTaskName(issue), projectHints.get(issue.project),defaultLabel);
            Optional<Task> maybe = backend.getMatchingTask(t -> t.content.equals(request.getIdentifier()));

            if (issue.isOpen() && maybe.isEmpty()) {
                // Case 1
                actions.add(() -> backend.create(request));
            } else if (!issue.isOpen() && maybe.isPresent()) {
                actions.add((() -> backend.complete(maybe.get())));
            }
        }

        for (Task task : existingIssues) {
            Optional<JiraIssue> thread = issues.stream()
                    .filter(s -> task.content.startsWith("[" + getTaskName(s) + "]"))
                    .findFirst();
            if (thread.isEmpty()  && ! task.isCompleted()) {
                // 4 - complete the task
                actions.add(() -> backend.complete(task));
            }
        }
        return actions;
    }

    String getTaskName(JiraIssue issue) {
        // checking on reporter first so the task will have consistent same title
        // no matter if user gets assigned in future or not.
        if (myself().equals(issue.reporter)) {
            return "Follow-up " + issue.title;
        } else if (myself().equals(issue.assignee)) {
            return "Fix " + issue.title;
        } else {
            return "Fix " + issue.title();
        }
    }

    private boolean isAJiraIssue(Task task) {
        return (task.content.startsWith("[Fix ") || task.content.startsWith("[Follow-up ")) && task.content.contains("](" + jiraURL);
    }
}

