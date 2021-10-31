package me.escoffier.timeless.todoist;

import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.NewTaskRequest;
import me.escoffier.timeless.model.Project;
import me.escoffier.timeless.model.Task;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ApplicationScoped
public class TodoistService implements Backend {

    private static final Logger LOGGER = Logger.getLogger("Todoist");

    private Project inbox;
    private List<Project> projects;
    private List<Task> tasks;

    @Inject @RestClient Todoist todoist;

    @PostConstruct
    void fetch() {
        SyncResponse response = todoist.sync(new SyncRequest());

        inbox = response.projects.stream().filter(p -> p.name.equalsIgnoreCase("Inbox"))
                .findFirst()
                .orElseThrow();

        projects = response.projects;
        response.items.forEach(t -> t.project = getProjectPerId(t.project_id));
        tasks = response.items;
    }

    public void addTask(String content, String deadline, Project project) {
        Todoist.TaskCreationRequest request = new Todoist.TaskCreationRequest();
        request.content = content;
        if (project != null) {
            request.project_id = project.id;
        }
        if (deadline != null) {
            request.due_string = deadline;
        }
        todoist.addTask(request);
    }

    public Project getProjectByName(String name) {
        return projects.stream().filter(p -> p.name.equalsIgnoreCase(name)).findFirst().orElseThrow();
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
    public Optional<Task> getTaskMatchingRequest(NewTaskRequest request) {
        return tasks.stream().filter(t -> t.content.equalsIgnoreCase(request.content)).findFirst();
    }

    @Override
    public void create(NewTaskRequest request) {
        LOGGER.infof("\uD83D\uDD04 Creating new task: %s", request.content);
        Project project = inbox;
        if (request.project != null) {
            project = getProjectByName(request.project);
        }
        addTask(request.content, request.due, project);
    }

    @Override
    public void complete(Task task) {
        LOGGER.infof("\uD83D\uDD04 Completing task: %s (%d)", task.content, task.id);
        todoist.completeTask(task.id);
    }
}
