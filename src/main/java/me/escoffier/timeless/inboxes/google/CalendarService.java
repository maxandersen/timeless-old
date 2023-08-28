package me.escoffier.timeless.inboxes.google;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import jakarta.annotation.PostConstruct;
import me.escoffier.timeless.helpers.ProjectHints;
import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.Inbox;
import me.escoffier.timeless.model.NewTaskRequest;
import me.escoffier.timeless.model.Task;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class CalendarService implements Inbox {

    private List<Meeting> fetched;

    @ConfigProperty(name = "meetings.ignored-meetings")
    List<String> ignored;

    @ConfigProperty(name = "meetings.hints")
    ProjectHints hints;

    @Inject GoogleAccounts accounts;
    @Inject Logger logger;

    @Override
    public List<Runnable> getPlan(Backend backend) {
        if (fetched == null) {
            fetch();
        }

        List<Task> existingMeetingTasks = backend.getAllMatchingTasks(this::isMeeting);

        // 1 - If fetched contains a meeting without an associated task -> create new task
        // 2 - If fetched contains a meeting with an uncompleted associated task -> do nothing
        // 3 - If fetched contains a meeting with a completed associated task -> do nothing
        // 4 - If backend contains "meeting" tasks (existingMeetingTasks) without an associated meeting in fetched -> complete task

        List<Runnable> actions = new ArrayList<>();
        for (Meeting meeting : fetched) {
            NewTaskRequest request = meeting.asNewTaskRequest(getProjectIfAny(meeting));
            Optional<Task> maybe = findTask(existingMeetingTasks, request);
            if (maybe.isEmpty()) {
                // Case 1
                actions.add(() -> backend.create(request));
            }
            // Case 2/3 - do nothing
        }


        for (Task task : existingMeetingTasks) {
            Optional<Meeting> meeting = fetched.stream().filter(s -> s.content().equalsIgnoreCase(task.content)).findFirst();
            if (meeting.isEmpty()  && ! task.isCompleted()) {
                // 4 - complete the task
                actions.add(() -> backend.complete(task));
            }
        }

        return actions;
    }

    private Optional<Task> findTask(List<Task> existing, NewTaskRequest request) {
        return existing.stream().filter(t -> t.content.startsWith(request.content)).findFirst();
    }

    private boolean isMeeting(Task task) {
        return task.content.contains("Prepare meeting '");
    }

    @PostConstruct
    public void fetch() {
        logger.info("\uD83D\uDEB6  Retrieving meeting from Google Calendars...");
        List<Meeting> messages = new ArrayList<>();
        for (Account account : accounts.accounts().values()) {
            messages.addAll(getMeetings(account));
        }
        fetched = new ArrayList<>(messages);
        removeIgnoredMeetings(fetched);
        logger.infof("\uD83D\uDEB6  %d meetings retrieved", fetched.size());
    }

    private void removeIgnoredMeetings(List<Meeting> fetched) {
        fetched.removeIf(m -> ignored.contains(m.getTitle()));
    }

    private String getProjectIfAny(Meeting meeting) {
        return hints.lookup(meeting.getTitle());
    }

    public Collection<Meeting> getMeetings(Account account) {
        try {
            LocalDate now = LocalDate.now();
            LocalDate week = now.plus(7, ChronoUnit.DAYS);
            List<Event> items = account.calendar().events().list("primary")
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .setTimeMin(new DateTime(new Date()))
                    .setTimeMax(new DateTime(week.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()))
                    .execute().getItems();
            List<Meeting> meetings = new ArrayList<>();
            for (Event item : items) {
                if (isCall(item)  && isAccepted(item)) {
                    Meeting meeting = new Meeting(item, item.getSummary(), item.getStart().getDateTime().toStringRfc3339());
                    meetings.add(meeting);
                } else {
                    if (! isAccepted(item)) {
                        logger.infof("\uD83D\uDE44  Ignoring meeting %s - Event has not been accepted", item.getSummary());
                    }
                }
            }
            return meetings;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to get calendar events", e);
        }
    }

    private boolean isCall(Event item) {
        return item.getHangoutLink() != null  || item.getLocation() != null  && item.getLocation().contains("meet.google.com");
    }

    private boolean isAccepted(Event item) {
        if (item.getCreator().isSelf()  && item.getStatus().equalsIgnoreCase("confirmed")) {
            return true;
        }
        List<EventAttendee> attendees = item.getAttendees();
        if (attendees == null) {
            return true;
        }
        for (EventAttendee attendee : attendees) {
            if (attendee.isSelf()) {
                return attendee.getResponseStatus().equalsIgnoreCase("accepted");
            }
        }
        return false;
    }

}
