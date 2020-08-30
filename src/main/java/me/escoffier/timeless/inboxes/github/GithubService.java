package me.escoffier.timeless.inboxes.github;

import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.Inbox;
import me.escoffier.timeless.model.NewTaskRequest;
import me.escoffier.timeless.model.Task;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GithubService implements Inbox {


    private static final Logger LOGGER = Logger.getLogger("Github");

    @Inject @RestClient GithubIssues github;
    private List<Issue> issues = new ArrayList<>();

    @PostConstruct
    public void fetch() {
        LOGGER.info("\uD83D\uDEB6  Retrieving Github Issues...");
        issues = github.getOpenIssuesAssignedToMe();
        LOGGER.infof("\uD83D\uDEB6  %d issues", issues.size());
    }

    @Override
    public List<Runnable> getPlan(Backend backend) {
        List<Task> existingIssues = backend.getMatchingTasks(this::isAGithubIssue);

        // 1 - If fetched contains an open issue without an associated task -> create new task
        // 2 - If fetched contains an open issue with an uncompleted associated task -> do nothing
        // 3 - If fetched contains a closed issue with an uncompleted associated task -> complete the task
        // 4 - If backend contains issue tasks (existingIssues) without an associated issue in fetched -> complete task

        List<Runnable> actions = new ArrayList<>();
        for (Issue issue : issues) {
            NewTaskRequest request = issue.asNewTaskRequest();
            Optional<Task> maybe = backend.getTaskMatchingRequest(request);

            if (! issue.isOpen()  && maybe.isPresent()) {
                // Case 3
                actions.add(() -> backend.complete(maybe.get()));
            } else if (issue.isOpen()  && maybe.isEmpty()) {
                // Case 1
                actions.add(() -> backend.create(request));
            } else if (! issue.isOpen()  && maybe.isPresent()) {
                actions.add((() -> backend.complete(maybe.get())));
            }
        }

        for (Task task : existingIssues) {
            Optional<Issue> thread = issues.stream()
                    .filter(s -> task.content.startsWith("[" + s.getTaskName() + "]"))
                    .findFirst();
            if (thread.isEmpty()) {
                // 4 - complete the task
                actions.add(() -> backend.complete(task));
            }
        }

        return actions;
    }

    private boolean isAGithubIssue(Task task) {
        return task.content.startsWith("[Fix ")  && task.content.contains("](https://github.com/");
    }
}
