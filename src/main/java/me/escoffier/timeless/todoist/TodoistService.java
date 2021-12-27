package me.escoffier.timeless.todoist;

import me.escoffier.timeless.model.*;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;

@ApplicationScoped
public class TodoistService implements Backend {

    private static final Logger LOGGER = Logger.getLogger("Todoist");

    private Project inbox;
    private List<Project> projects;
    private List<Task> tasks;
    private List<Label> labels;

    @Inject @RestClient Todoist todoist;

    @Inject @RestClient TodoistV8 v8;

    private List<Task> completed;

    @PostConstruct
    void fetch() {
        SyncResponse response = todoist.sync(new SyncRequest());

        ZonedDateTime time = Instant.now().minus(Duration.ofDays(7))
                .atZone(ZoneOffset.UTC)
                .with(HOUR_OF_DAY, 0).with(MINUTE_OF_HOUR, 0);
        String since = DateTimeFormatter.ISO_INSTANT.format(time);
        TodoistV8.CompletedTasksResponse completedTasks = v8.getCompletedTasks(new TodoistV8.CompletedTaskRequest(since));

        completed = completedTasks.toTasks();
        inbox = response.projects.stream().filter(p -> p.name.equalsIgnoreCase("Inbox"))
                .findFirst()
                .orElseThrow();

        projects = response.projects;
        response.items.forEach(t -> t.project = getProjectPerId(t.project_id));
        this.tasks = response.items;
        labels = response.labels;
    }

    public void addTask(String content, String deadline, Project project, int priority, List<String> labels, String description) {
        Todoist.TaskCreationRequest request = new Todoist.TaskCreationRequest();
        request.content = content;
        request.description = description;

        if (project != null) {
            request.project_id = project.id;
        }
        if (deadline != null) {
            request.due_string = deadline;
        }
        if (priority != -1) {
            request.priority = priority;
        }
        if (! labels.isEmpty()) {
            request.labels.addAll(getLabelIds(labels));
        }
        todoist.addTask(request);
    }

    private List<Long> getLabelIds(List<String> labels) {
        return labels.stream().map(s -> getLabelByName(s).id).collect(Collectors.toList());
    }

    private Label getLabelByName(String name) {
        var foundLabel = labels.stream().filter(l -> l.getShortName().trim().equalsIgnoreCase(name)).findAny();

        if(foundLabel.isPresent()) {
            return foundLabel.get();
        } else {
            LOGGER.infof("Creating label %s", name);
            return todoist.createLabel(new Todoist.LabelCreationRequest(name));
        }
    }

    public Project getProjectByName(String name) {
        return projects.stream().filter(p -> p.name.equalsIgnoreCase(name)).findFirst()
                .orElseThrow(() -> new RuntimeException("No project named " + name + " in " + projects.stream().map(p -> p.name).collect(Collectors.toList())));
    }

    public Project getProjectPerId(long id) {
        return projects.stream().filter(p -> p.id == id).findFirst().orElseThrow();
    }

    @Override
    public List<Task> getAllTasks() {
        return Collections.unmodifiableList(tasks);
    }

    @Override
    public List<Project> getProjects() {
        return Collections.unmodifiableList(projects);
    }

    @Override
    public List<Task> getMatchingTasks(Predicate<Task> predicate) {
        return tasks.stream().filter(predicate).collect(Collectors.toList());
    }

    @Override
    public List<Task> getAllMatchingTasks(Predicate<Task> predicate) {
        List<Task> active = getMatchingTasks(predicate);
        List<Task> completed = this.completed.stream().filter(predicate).collect(Collectors.toList());

        List<Task> result = new ArrayList<>();
        result.addAll(active);
        result.addAll(completed);
        return result;
    }

    @Override
    public Optional<Task> getMatchingTask(Predicate<Task> predicate) {
        return tasks.stream().filter(predicate).findAny();
    }

    @Override
    public Optional<Task> getTaskMatchingRequest(NewTaskRequest request) {
        return tasks.stream().filter(t -> t.content.equalsIgnoreCase(request.content)).findFirst();
    }

    @Override
    public void create(NewTaskRequest request) {
        LOGGER.infof("\uD83D\uDD04 Creating new task: %s", request.content + ": " + request.description);
        Project project = inbox;
        if (request.project != null) {
            project = getProjectByName(request.project);
        }
        addTask(request.content, request.due, project, request.priority, request.labels, request.description);
    }

    @Override
    public void complete(Task task) {
        LOGGER.infof("\uD83D\uDD04 Completing task: %s (%d)", task.content, task.id);
        todoist.completeTask(task.id);
    }
}
