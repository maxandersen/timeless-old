package me.escoffier.timeless.inboxes.github;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@RegisterRestClient(baseUri = "https://api.github.com/issues")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GithubIssues {

    default String lookupAuth() {
        return "token " + token();
    }

    static String token() {
        return ConfigProvider.getConfig().getOptionalValue("github.token", String.class)
                .orElseThrow(() -> new IllegalStateException("Missing `github.token` property"));
    }

    /**
     * @return the list of open issues assigned to me.
     */
    @GET
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    List<Issue> getOpenIssuesAssignedToMe();

}
