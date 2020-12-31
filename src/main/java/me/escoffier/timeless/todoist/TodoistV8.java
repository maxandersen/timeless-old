package me.escoffier.timeless.todoist;

import me.escoffier.timeless.helpers.Markdown;
import me.escoffier.timeless.model.Project;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RegisterRestClient(baseUri = "https://todoist.com/API/v8/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface TodoistV8 {

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

    }

    class CompletedItem {
        public String completed_date;
        public String content;
        public long id;
        public long project_id;
        public long task_id;
        public Long parent_id;


        public String title() {
            return Markdown.getText(content);
        }

        public TemporalAccessor getCompletionDate() {
            return DateTimeFormatter.ISO_INSTANT.parse(completed_date);
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
