package me.escoffier.timeless;

import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.Project;
import me.escoffier.timeless.todoist.Todoist;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Scanner;

@ApplicationScoped
@CommandLine.Command(name = "talk", description = "Create a 'talk' project")
public class TalkProjectCommand implements Runnable {

    @RestClient
    Todoist todoist;

    @ConfigProperty(name = "talk.parent-project-name", defaultValue = "Talks")
    String parentProjectName;

    @Inject
    Backend backend;

    @Override
    public void run() {
        Scanner sc = new Scanner(System.in);
        System.out.println("❓  What's the name of the event?");
        String eventName = sc.nextLine();

        System.out.println("❓  Is it a conference talk requiring travelling? (yes/no)");
        boolean requireTravel = readBoolean(sc);
        String location = null;
        String startDate;
        String endDate = null;
        if (requireTravel) {
            System.out.println("❓  Where does the conference take place?");
            location = sc.nextLine();
            System.out.println("❓  When does the event start? (dd/mm/yyyy)");
            startDate = sc.nextLine();
            System.out.println("❓  When does the event end? (dd/mm/yyyy)");
            endDate = sc.nextLine();
        } else {
            System.out.println("❓  When is the event? (dd/mm/yyyy)");
            startDate = sc.nextLine();
        }

        System.out.println("❓  Is there a CFP? (yes/no)");
        boolean needSubmission = readBoolean(sc);
        String submissionDeadline = null;
        String cfp = null;
        if (needSubmission) {
            System.out.println("❓  What's the CFP URL?");
            cfp = sc.nextLine();
            System.out.println("❓  When is the CFP deadline? (dd/mm/yyyy)");
            submissionDeadline = sc.nextLine();
        }

        // ----

        System.out.println("⚙️  Generating project for " + eventName + "...");
        Project parent = getParentProject();

        System.out.println("⚙️  Creating project " + parentProjectName + "/" + eventName);
        Todoist.ProjectCreationRequest pcr = new Todoist.ProjectCreationRequest(eventName, parent.id());
        Project project = todoist.createProject(pcr);

        System.out.println("project created: " + project.name() + " (" + project.id() + ")");
        if (requireTravel) {
            System.out.println("⚙️  Creating travel section...");
            Todoist.SectionCreationRequest scr = Todoist.SectionCreationRequest.create("Travel", project.id());
            Todoist.Section section = todoist.createSection(scr);
            // If travel - Estimate budget, Ask for budget, Wait for approval, Book travel, Book hotel, Add calendar slot (day - 7)
            System.out.println("⚙️  Creating travelling tasks...");
            Todoist.TaskCreationRequest tcr = Todoist.TaskCreationRequest.create(eventName + " - Estimate travel budget",
                    project.id(), section.id());
            todoist.addTask(tcr);

            tcr = tcr.withContent(eventName + " - Ask for budget");
            todoist.addTask(tcr);

            tcr = tcr.withContent(eventName + " - Wait for budget approval");
            todoist.addTask(tcr);

            tcr = tcr.withContent(eventName + " Book travel to " + location)
                    .withDescription("Start date: " + startDate + " - End date: " + endDate);
            todoist.addTask(tcr);

            tcr = tcr.withContent(eventName + " - Book hotel in " + location);
            todoist.addTask(tcr);

            tcr = tcr.withContent(eventName + " - Add calendar slot")
                    .withDescription(null)
                    .withDue("1 week before " + startDate);
            todoist.addTask(tcr);

            tcr = tcr.withContent(eventName + " - Expense Report")
                    .withDue("3 days after " + endDate).withPriority(3);
            todoist.addTask(tcr);

            System.out.println("⚙️  Travel section created!");
        }

        if (needSubmission) {
            System.out.println("⚙️  Creating CFP section...");
            Todoist.SectionCreationRequest scr = Todoist.SectionCreationRequest.create("CFP", project.id());
            Todoist.Section section = todoist.createSection(scr);
            // If submission (CFP website + deadline) - Write abstract, Submit abstract, Save title/abstract, Wait for acceptance
            System.out.println("⚙️  Creating CFP tasks...");
            Todoist.TaskCreationRequest tcr = Todoist.TaskCreationRequest.create(eventName + " - Write title and abstract", project.id(), section.id())
                    .withDue("1 day before " + submissionDeadline);
            todoist.addTask(tcr);

            tcr = tcr.withContent(eventName + " - Submit abstract and save it")
                    .withDescription(cfp);
            todoist.addTask(tcr);

            tcr = tcr.withContent(eventName + " - Wait for notification")
                    .withDue(null);
            todoist.addTask(tcr);

            System.out.println("⚙️  CFP section created!");
        }

        System.out.println("⚙️  Creating Material section...");
        Todoist.SectionCreationRequest scr = Todoist.SectionCreationRequest.create("Material", project.id());
        Todoist.Section section = todoist.createSection(scr);
        //  Slide title !!3, Slide audience !!3, Slide talk details !!3 , Slide call for action !!3, Outline !!2,
        // Slides parts A, B, C !!1, Outline demo !!2, Implement demo !!1, Share demo + Add link to slides
        System.out.println("⚙️  Creating Material tasks...");
        Todoist.TaskCreationRequest tcr = Todoist.TaskCreationRequest.create(eventName + " - Create slide deck files and directory", project.id(), section.id())
                .withPriority(3);
        todoist.addTask(tcr);

        tcr = tcr.withContent(eventName + " - Write down the objective, the audience and the outline of the talk")
                .withPriority(2);
        todoist.addTask(tcr);
        tcr = tcr.withContent(eventName + " -Write down the outline of the demo")
                .withPriority(2);
        todoist.addTask(tcr);

        tcr = tcr.withContent(eventName + " -Define and write the call for action slide")
                .withPriority(2);
        todoist.addTask(tcr);

        tcr = tcr.withContent(eventName + " - Slide part A")
                .withPriority(4);
        todoist.addTask(tcr);
        tcr = tcr.withContent(eventName + " - Slide part B")
                .withPriority(4);
        todoist.addTask(tcr);
        tcr = tcr.withContent(eventName + " - Slide part C")
                .withPriority(4);
        todoist.addTask(tcr);

        tcr = tcr.withContent(eventName + " - Implement Demo part A")
                .withPriority(4);
        todoist.addTask(tcr);
        tcr = tcr.withContent(eventName + " - Implement Demo part B")
                .withPriority(4);
        todoist.addTask(tcr);

        tcr = tcr.withContent(eventName + " - Share demo and slides")
                .withPriority(1)
                .withDue("1 day after " + endDate);
        todoist.addTask(tcr);

        System.out.println("⚙️  Material section created!");

        System.out.println("⚙️  Creating Preparation section...");
        scr = Todoist.SectionCreationRequest.create("Preparation", project.id());
        section = todoist.createSection(scr);

        System.out.println("⚙️  Creating preparation tasks...");
        tcr = Todoist.TaskCreationRequest.create(eventName + " - Dry Run 1", project.id(), section.id())
                .withDue("2 days before " + startDate)
                .withPriority(4);
        todoist.addTask(tcr);

        tcr = tcr.withContent(eventName + " - Dry Run 2")
                .withDue("1 days before " + startDate);
        todoist.addTask(tcr);
        System.out.println("\uD83D\uDE4C️  DONE!");
    }

    private Project getParentProject() {
        return backend.getProjects().stream().filter(p -> p.name().equalsIgnoreCase(parentProjectName)).findAny()
                .orElseThrow(() -> new RuntimeException("Unable to find the project " + parentProjectName));
    }

    private boolean readBoolean(Scanner sc) {
        String s = sc.nextLine().toLowerCase().trim();
        return s.equals("true") || s.equals("yes") || s.equals("y");
    }
}
