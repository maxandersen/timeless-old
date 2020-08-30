package me.escoffier.timeless.inboxes.pocket;

import java.util.HashMap;
import java.util.Map;

public class ReadList {

    private int status;
    private int complete;
    private String error;

    private Map<String, Item> list = new HashMap<>();

    public String getError() {
        return error;
    }

    public ReadList setError(String error) {
        this.error = error;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public ReadList setStatus(int status) {
        this.status = status;
        return this;
    }

    public int getComplete() {
        return complete;
    }

    public ReadList setComplete(int complete) {
        this.complete = complete;
        return this;
    }

    public Map<String, Item> getList() {
        return list;
    }

    public ReadList setList(Map<String, Item> list) {
        this.list = list;
        return this;
    }
}
