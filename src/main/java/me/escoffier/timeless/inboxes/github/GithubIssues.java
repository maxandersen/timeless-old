package me.escoffier.timeless.inboxes.github;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@RegisterRestClient(baseUri = "https://api.github.com/issues")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GithubIssues {

    String TOKEN = "c93145173d593174729cb72a4d74d7af42716525";

    default String lookupAuth() {
        return "token " + TOKEN;
    }

    /**
     * @return the list of open issues assigned to me.
     */
    @GET
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    List<Issue> getOpenIssuesAssignedToMe();


}
