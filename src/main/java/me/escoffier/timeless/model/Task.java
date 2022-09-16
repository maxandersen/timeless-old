package me.escoffier.timeless.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;


public class Task {

    public String id;
    public String content;
    public String project_id;
    public int priority;
    public String parentTaskId;
    public boolean checked;
    public Due due;
    public String date_added;
    public String[] labels;

    public Project project;


    public Task() {
        // Used by jsonb
    }

    public boolean isCompleted() {
        return checked;
    }

    public Instant getCreationDate() {
        return Instant.from(ISO_OFFSET_DATE_TIME.parse(date_added));
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
