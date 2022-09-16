package me.escoffier.timeless.review;

import me.escoffier.timeless.model.Project;
import me.escoffier.timeless.todoist.Todoist;
import me.escoffier.timeless.todoist.TodoistV9;
import org.apache.commons.io.output.StringBuilderWriter;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;

public class ReviewHelper {

    public static void prepareWeeklyReview(String reviewProjectName, Todoist todoist, TodoistV9 todoistV9,
            List<Project> projects) {
        Project weeklyReviewProject = null;
        for (Project project : projects) {
            if (project.name.equalsIgnoreCase(reviewProjectName)) {
                weeklyReviewProject = project;
                break;
            }
        }
        if (weeklyReviewProject == null) {
            throw new NoSuchElementException("Unable to find weekly review project: " + reviewProjectName);
        }

        List<TodoistV9.CompletedItem> subTasks = new ArrayList<>();
        ZonedDateTime time = Instant.now().minus(Duration.ofDays(10))
                .atZone(ZoneOffset.UTC)
                .with(HOUR_OF_DAY, 0).with(MINUTE_OF_HOUR, 0);
        String since = DateTimeFormatter.ISO_INSTANT.format(time);
        TodoistV9.CompletedTasksResponse tasks = todoistV9
                .getCompletedTasks(new TodoistV9.CompletedTaskRequest(since));
        for (TodoistV9.CompletedItem item : tasks.items) {
            if (item.project_id.equals(weeklyReviewProject.id) && item.completed_date != null) {
                subTasks.add(item);
            }
        }

        subTasks.forEach(ci -> todoist.uncompleteTask(ci.task_id));

    }

    public static void toHtml(StringBuilderWriter writer) throws IOException {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(writer.toString());
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        StringBuilderWriter report = new StringBuilderWriter();
        renderer.render(document, report);
        Files.write(new File("weekly.html").toPath(), report.toString().getBytes(StandardCharsets.UTF_8));
    }
}
