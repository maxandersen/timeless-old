package me.escoffier.timeless.helpers;

import java.util.List;

public class ProjectHints {

    public final List<ProjectHint> hints;

    public ProjectHints(List<ProjectHint> hints) {
        this.hints = hints;
    }

    public String lookup(String content) {
        return hints.stream().filter(ph -> ph.match(content)).map(ph -> ph.project).findAny().orElse(null);
    }
}
