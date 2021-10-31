package me.escoffier.timeless.todoist;

import java.util.Arrays;
import java.util.List;

public class SyncRequest {

    public String token;
    public String sync_token;
    public List<String> resource_types;

    public SyncRequest() {
        token = Todoist.token();
        sync_token = "*";
        resource_types = Arrays.asList("items", "projects", "labels");
    }

    public static final SyncRequest INSTANCE = new SyncRequest();

}
