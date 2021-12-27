package me.escoffier.timeless.model;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface Backend {

    // Queries:

    List<Task> getAllTasks();

    List<Project> getProjects();

    List<Task> getMatchingTasks(Predicate<Task> predicate);

    /**
     * Unlike {@link #getMatchingTasks(Predicate)}, also cover the completed tasks
     * @param predicate the predicate, must not be {@code null}
     * @return the list of matching tasks, potentially containing completed tasks
     */
    List<Task> getAllMatchingTasks(Predicate<Task> predicate);

    Optional<Task> getMatchingTask(Predicate<Task> predicate);

    Optional<Task> getTaskMatchingRequest(NewTaskRequest request);

    // Actions:

    void create(NewTaskRequest request);

    void complete(Task task);
}
