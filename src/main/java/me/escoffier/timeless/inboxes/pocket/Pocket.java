package me.escoffier.timeless.inboxes.pocket;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(baseUri = "https://getpocket.com/v3")
@Produces(MediaType.MEDIA_TYPE_WILDCARD)
public interface Pocket {

    @POST
    @Path("/get")
    @ClientHeaderParam(name = "Content-Type", value = "application/json")
    ReadList getReadList(RetrieveRequest request);

    class RetrieveRequest {
        public final String consumer_key = getConsumerKey();
        public final String access_token = getAccessToken();
        public final String count = "500";
        public final String detailType = "complete";
        public final String state = "unread";

        public static final RetrieveRequest INSTANCE = new RetrieveRequest();
    }

    static String getConsumerKey() {
        return ConfigProvider.getConfig().getOptionalValue("pocket.consumer-key", String.class)
                .orElseThrow(() -> new IllegalStateException("Missing `pocket.consumer-key` property"));
    }

    static String getAccessToken() {
        return ConfigProvider.getConfig().getOptionalValue("pocket.access-token", String.class)
                .orElseThrow(() -> new IllegalStateException("Missing `pocket.access-token` property"));
    }

}
