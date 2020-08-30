package me.escoffier.timeless.todoist;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@RegisterRestClient(baseUri = "https://api.todoist.com")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface Todoist {

    String TOKEN = "5b41f0e89138743efd5f71b554d119d561b6eeda";

    @POST
    @Path("/sync/v8/sync")
    SyncResponse sync(SyncRequest request);

    @POST
    @Path("/rest/v1/tasks")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    void addTask(TaskCreationRequest request);

    @POST
    @Path("/rest/v1/tasks/{id}/close")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    void completeTask(@PathParam("id") long id);

    default String lookupAuth() {
        return "Bearer " + TOKEN;
    }

    class TaskCreationRequest {

        public String content;
        public String due_string;
        public int priority = 1;
        public long project_id;

    }
}
