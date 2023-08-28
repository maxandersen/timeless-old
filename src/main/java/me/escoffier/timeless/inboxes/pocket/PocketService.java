package me.escoffier.timeless.inboxes.pocket;

import jakarta.annotation.PostConstruct;
import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.Inbox;
import me.escoffier.timeless.model.NewTaskRequest;
import me.escoffier.timeless.model.Task;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PocketService implements Inbox {

    private static final Logger LOGGER = Logger.getLogger("Pocket");
    private static final String TOO_MANY_ARTICLES = "Review your reading list";

    private ReadList list;

    @Inject @RestClient Pocket pocket;

    @ConfigProperty(name = "pocket.limit", defaultValue = "100") int limit;

    @PostConstruct
    public void fetch() {
        LOGGER.info("\uD83D\uDEB6 Retrieving reading list from Pocket...");
        list = pocket.getReadList(Pocket.RetrieveRequest.INSTANCE);
        LOGGER.infof("\uD83D\uDEB6  Read List size: %d", list.getList().size());
    }

    @Override
    public List<Runnable> getPlan(Backend backend) {
        if (list == null) {
            fetch();
        }

        List<Task> existingReadTasks = backend.getMatchingTasks(this::isFromReadingList);

        // 1 - If fetched contains an item without an associated task -> create new task
        // 2 - If fetched contains an item with an uncompleted associated task -> do nothing
        // 3 - If backend contains "read" tasks (existingReadTasks) without an associated item in fetched -> complete task

        List<Runnable> actions = new ArrayList<>();
        for (Item item : list.getList().values()) {
            NewTaskRequest request = item.asNewTaskRequest();
            Optional<Task> maybe = backend.getTaskMatchingRequest(request);
            if (maybe.isEmpty()) {
                // Case 1
                actions.add(() -> backend.create(request));
            }
        }

        for (Task task : existingReadTasks) {
            Optional<Item> thread = list.getList().values().stream()
                    .filter(s -> task.content.startsWith("[" + s.getTaskTitle() + "]"))
                    .findFirst();
            if (thread.isEmpty()) {
                // 4 - complete the task
                actions.add(() -> backend.complete(task));
            }
        }

        if (list.getList().keySet().size() >= limit  && backend.getMatchingTasks(t -> t.content.equalsIgnoreCase(TOO_MANY_ARTICLES)).isEmpty()) {
            actions.add(() -> backend.create(new NewTaskRequest(TOO_MANY_ARTICLES, Item.READING_LIST_PROJECT, "sunday")));
        }

        return actions;
    }

    private boolean isFromReadingList(Task task) {
        return task.content.contains("](https://app.getpocket.com/read/");
    }
}
