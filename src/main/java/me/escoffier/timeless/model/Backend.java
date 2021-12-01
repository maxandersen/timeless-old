package me.escoffier.timeless.model;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface Backend {

    // Queries:

    List<Task> getAllTasks();

    List<Project> getProjects();

    List<Task> getMatchingTasks(Predicate<Task> predicate);

    Optional<Task> getMatchingTask(Predicate<Task> predicate);

    Optional<Task> getTaskMatchingRequest(NewTaskRequest request);

    // Actions:

    void create(NewTaskRequest request);

    void complete(Task task);
}
