package me.escoffier.timeless.inboxes.gmail;

import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.Inbox;
import me.escoffier.timeless.model.NewTaskRequest;
import me.escoffier.timeless.model.Task;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GmailService implements Inbox {

    private static final Logger LOGGER = Logger.getLogger("GmailService");

    private final Account personal;
    private final Account redhat;

    private List<StarredThread> fetched;

    public GmailService() {
        personal = new Account("personal", "token-personal", 8888);
        redhat = new Account("redhat", "token-redhat", 8889);
    }

    @Override
    public List<Runnable> getPlan(Backend backend) {
        if (fetched == null) {
            fetch();
        }

        List<Task> existingEmailTasks = backend.getMatchingTasks(this::isEmail);


        // 1 - If fetched contains an email without an associated task -> create new task
        // 2 - If fetched contains an email with an uncompleted associated task -> do nothing
        // 3 - If fetched contains an email with an completed associated task -> remove star (cannot be implemented as you cannot retrieve completed tasks)
        // 4 - If backend contains "email" tasks (existingEmailTasks) without an associated email in fetched -> complete task

        List<Runnable> actions = new ArrayList<>();
        for (StarredThread starred : fetched) {
            NewTaskRequest request = starred.asNewTaskRequest();
            Optional<Task> maybe = backend.getTaskMatchingRequest(request);
            if (maybe.isEmpty()) {
                // Case 1
                actions.add(() -> backend.create(request));
                continue;
            }
            Task task = maybe.get();
            if (task.isCompleted()) {
                // Case 3
                actions.add(() -> unflag(starred));
            }
        }


        for (Task task : existingEmailTasks) {
            Optional<StarredThread> thread = fetched.stream().filter(s -> s.content().equalsIgnoreCase(task.content)).findFirst();
            if (thread.isEmpty()) {
                // 4 - complete the task
                actions.add(() -> backend.complete(task));
            }
        }

        return actions;
    }

    private boolean isEmail(Task task) {
        return task.content.contains("](https://mail.google.com/mail/u/");
    }

    @PostConstruct
    public List<StarredThread> fetch() {
        LOGGER.info("\uD83D\uDEB6  Retrieving starred emails from Gmail...");
        try {
            Collection<StarredThread> messages = new ArrayList<>(personal.getStarredMessages());
            messages.addAll(redhat.getStarredMessages());
            fetched = new ArrayList<>(messages);
            LOGGER.infof("\uD83D\uDEB6  %d starred emails retrieved", fetched.size());
            return fetched;
        } catch (Exception e) {
            throw new IllegalStateException("\uD83D\uDC7F Unable to retrieve messages from GMAIL", e);
        }
    }

    public void unflag(StarredThread starred) {
        try {
            starred.account().unflag(starred);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
