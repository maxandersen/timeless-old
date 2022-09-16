package me.escoffier.timeless;

import me.escoffier.timeless.inboxes.jira.JiraService;
import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.Inbox;
import me.escoffier.timeless.model.Task;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@CommandLine.Command(name = "sync", description = "Sync Todoist tasks with Gmail, Github, Pocket...")
public class SyncCommand implements Runnable {

    private static final Logger LOGGER = Logger.getLogger("Timeless");

    @Inject Backend backend;

    @Inject Instance<Inbox> inboxes;

    @Override
    public void run() {
        List<Runnable> plan = new ArrayList<>();

        inboxes.forEach(i -> plan.addAll(i.getPlan(backend)));

        LOGGER.infof("\uD83D\uDEB6 Executing plan containing %d actions", plan.size());
        plan.forEach(r -> {
            try {
                r.run();
            } catch (RuntimeException e) {
                LOGGER.error("\uD83D\uDC7F Unable to execute an action", e);
            }
        });

        // Find duplicates
        List<Task> tasks = backend.getAllTasks();
        for (Task t : tasks) {
            String content = t.content;
            Optional<Task> optional = tasks.stream()
                    .filter(other -> other != t && other.content.equalsIgnoreCase(content))
                    .findFirst();
            optional.ifPresent(task -> LOGGER.warnf("\uD83E\uDD14 Duplicate tasks found: %s (%s)", task.content,
                    task.project == null ? "inbox" : task.project.name()));
        }

    }

}
