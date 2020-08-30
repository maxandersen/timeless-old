package me.escoffier.timeless.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Task {

    public long id;
    public String content;
    public long project_id;
    public int priority;
    public String parentTaskId;
    public int checked;
    public Due deadline;

    public Project project;


    public Task() {
        // Used by jsonb
    }

    public boolean isCompleted() {
        return checked == 1;
    }


    public boolean isInInbox() {
        return project == null  || project.name.equalsIgnoreCase("inbox");
    }

    @Override
    public String toString() {
        return content + " (" + id + ")";
    }

    public static String sanitize(String name) {
        Objects.requireNonNull(name);

        if (name.startsWith("\"") && name.endsWith("\"")) {
            name = name.substring(1, name.length() - 1);
        }

        name = name
                .replace("!", "")
                .replace("#", "")
                .replace("@", "")
                .replace("::", "")
                .replace(":", " ")
                .replace("\n", "")
                .replace("\t", " ")
                .replace("\"", "`")
                .replace("'", "`")
                .replace("//", " ")
                .replace("  ", " ")
                .replace("%20", " ")
                .trim();

        return name;
    }
}
