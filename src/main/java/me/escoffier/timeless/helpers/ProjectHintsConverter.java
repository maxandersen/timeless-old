package me.escoffier.timeless.helpers;

import org.eclipse.microprofile.config.spi.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectHintsConverter implements Converter<ProjectHints> {

    @Override
    public ProjectHints convert(String value) throws IllegalArgumentException, NullPointerException {
        List<ProjectHint> list = value.lines().map(s -> toProjectHint(s.trim())).collect(Collectors.toList());
        return new ProjectHints(list);
    }

    private ProjectHint toProjectHint(String line) {
        int idx = line.indexOf(":");
        String project = line.substring(0, idx).trim();
        if (project.startsWith("- ")) {
            project = project.substring(2);
        }
        List<String> hints = Arrays.stream(line.substring(idx+1).split(","))
                .map(s -> s.toLowerCase().trim()).collect(Collectors.toList());
        return new ProjectHint(project, hints);
    }
}
