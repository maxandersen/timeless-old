package me.escoffier.timeless.todoist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import me.escoffier.timeless.model.Label;
import me.escoffier.timeless.model.Project;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@RegisterRestClient(baseUri = "https://api.todoist.com")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface Todoist {

    @POST
    @Path("/sync/v9/sync")
    SyncResponse sync(SyncRequest request);

    @POST
    @Path("/rest/v2/tasks")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    void addTask(TaskCreationRequest request);

    @POST
    @Path("/rest/v2/projects")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    Project createProject(ProjectCreationRequest request);

    @POST
    @Path("/rest/v2/labels")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    Label createLabel(LabelCreationRequest request);

    @POST
    @Path("/rest/v1/sections") // Stay on v1, failing on v2.
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    Section createSection(SectionCreationRequest request);

    @POST
    @Path("/rest/v2/tasks/{id}/close")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    void completeTask(@PathParam("id") String id);

    @POST
    @Path("/rest/v2/tasks/{id}/reopen")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    void uncompleteTask(@PathParam("id") long id);

    default String lookupAuth() {
        return "Bearer " + token();
    }

    static String token() {
        return ConfigProvider.getConfig().getValue("todoist.token", String.class);
    }

    class TaskCreationRequest {

        public String content;
        public String due_string;
        public int priority = 1;

        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        public String project_id;

        @JsonProperty("label_ids")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public List<String> labels = new ArrayList<>();

        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        public String section_id;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public String description;

    }

    class ProjectCreationRequest {
        public String name;
        public String parent_id;
    }

    class SectionCreationRequest {
        public String name;
        public long project_id;

        public SectionCreationRequest(String name, String project_id) {
            this.name = name;
            this.project_id = Long.parseLong(project_id);
        }
    }

    class Section {
        public String id;
        public String project_id;
        public String name;
    }

    class LabelCreationRequest {
        public String name;

        public LabelCreationRequest(String name) {
            this.name = name;
        }
        //todo: order, color, favorite
    }

}
