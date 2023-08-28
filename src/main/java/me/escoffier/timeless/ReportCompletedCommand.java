package me.escoffier.timeless;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import me.escoffier.timeless.model.Project;
import me.escoffier.timeless.todoist.TodoistV9;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;

@ApplicationScoped
@CommandLine.Command(name = "report", description = "List completed tasks over the past 8 days")
public class ReportCompletedCommand implements Runnable {

    @Inject @RestClient
    TodoistV9 todoist;

    @Inject @ConfigProperty(name = "report.excluded_projects") List<String> excludedProjects;

    @Override
    public void run() {
        ZonedDateTime time = Instant.now().minus(Duration.ofDays(7))
                .atZone(ZoneOffset.UTC)
                .with(HOUR_OF_DAY, 0).with(MINUTE_OF_HOUR, 0);
        String since = DateTimeFormatter.ISO_INSTANT.format(time);
        TodoistV9.CompletedTaskRequest req = new TodoistV9.CompletedTaskRequest(since);
        TodoistV9.CompletedTasksResponse resp = todoist.getCompletedTasks(req);
        Map<String, Project> projects = resp.projects;
        List<TodoistV9.CompletedItem> items = resp.items;

        ListMultimap<String, String> report = MultimapBuilder.treeKeys((Comparator<String>) (o1, o2) -> {
            if (o1.equalsIgnoreCase("inbox")  && o2.equalsIgnoreCase("inbox")) {
                return 0;
            }
            if (o1.equalsIgnoreCase("inbox")) {
                return -1;
            }
            if (o2.equalsIgnoreCase("inbox")) {
                return 1;
            }
            return o1.compareTo(o2);
        }).arrayListValues().build();

        for (TodoistV9.CompletedItem completed : items) {
            Project project = null;
            if (completed.project_id != null) {
                project = projects.get(completed.project_id);
            }

            // Ignore dependabot
            if (completed.title().startsWith("Review PR Bump ")) {
                continue;
            }

            if (project == null) {
                report.put("inbox", completed.title());
            } else if (!excludedProjects.contains(project.name())) {
                String pn = getProjectName(projects, project);
                report.put(pn, completed.title());
            }
        }

        Set<String> added = new HashSet<>();
        report.asMap().forEach((key, value) -> {
            System.out.println("== " + key);
            value.forEach(s -> {
                String extracted = extract(s);
                if (added.add(extracted)) {
                    System.out.println("    * " + extracted);
                }
            });
            System.out.println();
        });
    }



    private String extract(String s) {
        if (s.startsWith("[")  && s.lastIndexOf("]") != -1) {
            return s.substring(1, s.lastIndexOf(("]")));
        }
        return s;
    }

    private String getProjectName(Map<String, Project> projects, Project project) {
        if (project.parent() == null) {
            return  project.name();
        }
        Project parent = projects.values().stream().filter(p -> p.id().equalsIgnoreCase(project.parent())).findAny()
                .orElse(null);
        if (parent == null) {
            return project.name();
        }
        return parent.name() + "/" + project.name();
    }

}
