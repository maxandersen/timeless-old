package me.escoffier.timeless.inboxes.google;

import me.escoffier.timeless.helpers.ProjectHints;
import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.Inbox;
import me.escoffier.timeless.model.NewTaskRequest;
import me.escoffier.timeless.model.Task;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.util.*;

@ApplicationScoped
public class CalendarService implements Inbox {

    private static final Logger LOGGER = Logger.getLogger("CalendarService");

    private final Account personal;
    private final Account redhat;

    private List<Meeting> fetched;

    @ConfigProperty(name = "meetings.ignored-meetings")
    List<String> ignored;

    @ConfigProperty(name = "meetings.hints")
    ProjectHints hints;

    public CalendarService() {
        LOGGER.info("Setting up personal calendar account");
        personal = new Account("personal", "token-personal", 8888);
        LOGGER.info("Setting up redhat calendar account");
        redhat = new Account("redhat", "token-redhat", 8889);
    }

    @Override
    public List<Runnable> getPlan(Backend backend) {
        if (fetched == null) {
            fetch();
        }

        List<Task> existingMeetingTasks = backend.getMatchingTasks(this::isMeeting);

        // 1 - If fetched contains a meeting without an associated task -> create new task
        // 2 - If fetched contains a meeting with an uncompleted associated task -> do nothing
        // 3 - If fetched contains a meeting with a completed associated task -> do nothing
        // 4 - If backend contains "meeting" tasks (existingMeetingTasks) without an associated meeting in fetched -> complete task

        List<Runnable> actions = new ArrayList<>();
        for (Meeting meeting : fetched) {
            NewTaskRequest request = meeting.asNewTaskRequest(getProjectIfAny(meeting));
            Optional<Task> maybe = backend.getTaskMatchingRequest(request);
            if (maybe.isEmpty()) {
                // Case 1
                actions.add(() -> backend.create(request));
            }
            // Case 2/3 - do nothing
        }


        for (Task task : existingMeetingTasks) {
            Optional<Meeting> thread = fetched.stream().filter(s -> s.content().equalsIgnoreCase(task.content)).findFirst();
            if (thread.isEmpty()) {
                // 4 - complete the task
                actions.add(() -> backend.complete(task));
            }
        }

        return actions;
    }

    private boolean isMeeting(Task task) {
        return task.content.contains("Prepare meeting '");
    }

    @PostConstruct
    public List<Meeting> fetch() {
        LOGGER.info("\uD83D\uDEB6  Retrieving meeting from Google Calendars...");
        try {
            Collection<Meeting> messages = new ArrayList<>(personal.getMeetings());
            messages.addAll(redhat.getMeetings());
            fetched = new ArrayList<>(messages);
            removeIgnoredMeetings(fetched);
            LOGGER.infof("\uD83D\uDEB6  %d meetings retrieved", fetched.size());
            return fetched;
        } catch (Exception e) {
            throw new IllegalStateException("\uD83D\uDC7F Unable to retrieve meeting from Google calendar", e);
        }
    }

    private void removeIgnoredMeetings(List<Meeting> fetched) {
        fetched.removeIf(m -> ignored.contains(m.getTitle()));
    }

    private String getProjectIfAny(Meeting meeting) {
        return hints.lookup(meeting.getTitle());
    }

}
