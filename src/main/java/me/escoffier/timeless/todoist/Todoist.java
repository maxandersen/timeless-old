package me.escoffier.timeless.todoist;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RegisterRestClient(baseUri = "https://api.todoist.com")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface Todoist {

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

    @POST
    @Path("/rest/v1/tasks/{id}/reopen")
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
        public long project_id;

    }
}
