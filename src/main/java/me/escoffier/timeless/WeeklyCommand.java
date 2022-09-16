package me.escoffier.timeless;

import me.escoffier.timeless.model.Label;
import me.escoffier.timeless.model.Project;
import me.escoffier.timeless.model.Task;
import me.escoffier.timeless.review.ReviewHelper;
import me.escoffier.timeless.todoist.SyncRequest;
import me.escoffier.timeless.todoist.SyncResponse;
import me.escoffier.timeless.todoist.Todoist;
import me.escoffier.timeless.todoist.TodoistV9;
import org.apache.commons.io.output.StringBuilderWriter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
@CommandLine.Command(name = "weekly", description = "Prepare weekly review")
public class WeeklyCommand implements Runnable {

    @Inject @RestClient Todoist todoist;

    @Inject @RestClient
    TodoistV9 todoistV9;

    @ConfigProperty(name = "todoist.weekly-label") String weekly;

    @ConfigProperty(name = "todoist.waiting-label") String waiting;

    @ConfigProperty(name = "todoist.weekly-review") String reviewProjectName;

    @ConfigProperty(name = "todoist.excluded-upcoming-projects") List<String> excludedProjectsFromUpcoming;


    private StringBuilderWriter writer;

    @Override
    public void run() {
        writer = new StringBuilderWriter();
        SyncResponse response = todoist.sync(SyncRequest.INSTANCE);
        Label weekly = getWeekly(response.labels);
        Label wait = getWaitFor(response.labels);

        Map<Project, List<Task>> newTasks = new LinkedHashMap<>();
        Map<Project, List<Task>> waitingTasks = new LinkedHashMap<>();
        Map<Project, List<Task>> weeklyTasks = new LinkedHashMap<>();
        Map<Project, List<Task>> upcomingDeadlines = new LinkedHashMap<>();
        Instant lastWeek = Instant.now().minus(Duration.ofDays(7));
        LocalDate nextWeek = LocalDate.now().plusDays(15);

        int newTaskCount = 0;
        int weeklyTaskCount = 0;
        int waitingTaskCount = 0;
        int upcomingTasksCount = 0;

        for (Task item : response.items) {

            String projectId = item.project_id;
            for (Project p : response.projects) {
                if (p.id.equals(projectId)) {
                    item.project = p;
                    break;
                }
            }

            if (item.getCreationDate().isAfter(lastWeek)) {
                // filter out waiting items
                if (!hasLabel(item, wait)) {
                    newTaskCount = newTaskCount + 1;
                    addTask(newTasks, item);
                }
            }

            if (item.due != null && item.due.getDeadline().isBefore(nextWeek)) {
                if (item.project == null  || ! excludedProjectsFromUpcoming.contains(item.project.name)) {
                    addTask(upcomingDeadlines, item);
                    upcomingTasksCount = upcomingTasksCount + 1;
                }
            }

            if (hasLabel(item, wait)) {
                addTask(waitingTasks, item);
                waitingTaskCount = waitingTaskCount + 1;
            }

            if (hasLabel(item, weekly)) {
                addTask(weeklyTasks, item);
                weeklyTaskCount = weeklyTaskCount + 1;
            }
        }

        if (!newTasks.isEmpty()) {
            writer.append("## New tasks\n");
            writer.append(String.format("%d new tasks created this week and not completed:\n", newTaskCount));
            print(newTasks);
        }

        writer.append("---\n");

        if (!upcomingDeadlines.isEmpty()) {
            writer.append("## Upcoming deadlines\n");
            writer.append(String.format("%d items with upcoming deadlines:\n", upcomingTasksCount));
            print(upcomingDeadlines);
            writer.append("---\n");
        }

        if (!weeklyTasks.isEmpty()) {
            writer.append("## Still on the weekly list\n");
            writer.append(String.format("%d items marked for this week not yet completed\n", weeklyTaskCount));
            print(weeklyTasks);
        } else {
            writer.append("## Still on the weekly list\n");
            writer.append("All the weekly tasks are completed!");
        }

        writer.append("---\n");

        if (!waitingTasks.isEmpty()) {
            writer.append("## Waiting list\n");
            writer.append(String.format("%d items are in a _waiting_ state\n", waitingTaskCount));
            print(waitingTasks);
        }

        try {
            ReviewHelper.toHtml(writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        ReviewHelper.prepareWeeklyReview(reviewProjectName, todoist, todoistV9, response.projects);

    }

    public void print(Map<Project, List<Task>> newTasks) {
        writer.append("\n");
        for (Map.Entry<Project, List<Task>> entry : newTasks.entrySet()) {
            writer.append(String.format("* %s\n", entry.getKey().name));
            for (Task task : entry.getValue()) {
                writer.append(String.format("\t * %s", task.content));
                if (task.due != null) {
                    writer.append(String.format(" (due _%s_)\n", DateTimeFormatter.ISO_LOCAL_DATE.format(task.due.getDeadline())));
                } else {
                    writer.append("\n");
                }
            }
        }
        writer.append("\n");
    }

    private boolean hasLabel(Task item, Label label) {
        for (String l : item.labels) {
            if (l.equals(label.id)) {
                return true;
            }
        }
        return false;
    }

    private void addTask(Map<Project, List<Task>> container, Task item) {
        List<Task> list = container.computeIfAbsent(item.project, (p) -> new ArrayList<>());
        list.add(item);
    }

    Label getWeekly(List<Label> labels) {
        for (Label label : labels) {
            if (label.getShortName().equalsIgnoreCase(weekly)) {
                return label;
            }
        }
        throw new IllegalStateException("Unable to find `" + weekly + "` label in " + labels);
    }

    Label getWaitFor(List<Label> labels) {
        for (Label label : labels) {
            if (label.getShortName().equalsIgnoreCase(waiting)) {
                return label;
            }
        }
        throw new IllegalStateException("Unable to find `" + waiting + "` label in " + labels);
    }

}
