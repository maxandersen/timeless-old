package me.escoffier.timeless.todoist;

import me.escoffier.timeless.model.Label;
import me.escoffier.timeless.model.Project;
import me.escoffier.timeless.model.Task;

import java.util.List;

public record SyncResponse(List<Task> items, List<Project> projects, List<Label> labels) {

}
