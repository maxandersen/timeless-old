package me.escoffier.timeless.inboxes.pocket;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@RegisterRestClient(baseUri = "https://getpocket.com/v3")
@Produces(MediaType.MEDIA_TYPE_WILDCARD)
public interface Pocket {

    String CONSUMER_KEY = "57479-e7678a40906f0cf20b3ab989";
    String ACCESS_TOKEN = "f0d38067-c6cc-0ac1-5959-518ad1";


    @POST
    @Path("/get")
    @ClientHeaderParam(name = "Content-Type", value = "application/json")
    ReadList getReadList(RetrieveRequest request);

    class RetrieveRequest {
        public final String consumer_key = CONSUMER_KEY;
        public final String access_token = ACCESS_TOKEN;
        public final String count = "500";
        public final String detailType = "complete";
        public final String state = "unread";

        public static final RetrieveRequest INSTANCE = new RetrieveRequest();
    }


}
