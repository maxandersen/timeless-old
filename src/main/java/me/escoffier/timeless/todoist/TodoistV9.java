package me.escoffier.timeless.todoist;

import me.escoffier.timeless.helpers.Markdown;
import me.escoffier.timeless.model.Project;
import me.escoffier.timeless.model.Task;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RegisterRestClient(baseUri = "https://todoist.com/API/v9/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface TodoistV9 {

    @POST
    @Path("completed/get_all")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    CompletedTasksResponse getCompletedTasks(CompletedTaskRequest request);

    default String lookupAuth() {
        return "Bearer " + Todoist.token();
    }

    class CompletedTasksResponse {

        public List<CompletedItem> items;
        public Map<String, Project> projects;

        public List<Task> toTasks() {
            return items.stream().map(CompletedItem::toTask).collect(Collectors.toList());
        }
    }

    class CompletedItem {
        public String completed_date;
        public String content;
        public String id;
        public String project_id;
        public long task_id;
        public String parent_id;


        public String title() {
            return Markdown.getText(content);
        }

        public TemporalAccessor getCompletionDate() {
            return DateTimeFormatter.ISO_INSTANT.parse(completed_date);
        }

        public Task toTask() {
            Task task = new Task();
            task.content = title();
            task.id = id;
            task.checked = true;
            if (parent_id != null) {
                task.parentTaskId = parent_id;
            }
            if (project_id != null) {
                task.project_id = project_id;
            }
            return task;
        }
    }

    class CompletedTaskRequest {
        public final String since;
        public int limit = 200;

        public CompletedTaskRequest(String since) {
            this.since = since;
        }
    }
}
