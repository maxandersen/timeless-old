package me.escoffier.timeless.todoist;

import me.escoffier.timeless.model.Project;
import me.escoffier.timeless.model.Task;

import java.util.Collections;
import java.util.List;

public class SyncResponse {

    public List<Task> items;
    public List<Project> projects;


    public SyncResponse() {

    }


}
