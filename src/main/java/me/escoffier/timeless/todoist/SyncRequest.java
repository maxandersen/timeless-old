package me.escoffier.timeless.todoist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SyncRequest {

    public final String token;
    public final String sync_token;
    public final List<String> resource_types;

    private SyncRequest() {
        token = Todoist.TOKEN;
        sync_token = "*";
        resource_types = Arrays.asList("items", "projects");
    }

    public static final SyncRequest INSTANCE = new SyncRequest();

}
