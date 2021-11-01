package me.escoffier.timeless.helpers;

import java.util.List;
import java.util.stream.Collectors;

public class ProjectHint {

    public final String project;
    private final List<String> hints;

    public ProjectHint(String project, List<String> hints) {
        this.project = project;
        this.hints = hints.stream().map(String::toLowerCase).map(String::trim).collect(Collectors.toList());
    }

    public boolean match(String content) {
        return hints.stream().anyMatch(s -> content.toLowerCase().contains(s));
    }
}
