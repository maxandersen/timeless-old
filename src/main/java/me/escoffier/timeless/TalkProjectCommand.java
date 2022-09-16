package me.escoffier.timeless;

import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.Project;
import me.escoffier.timeless.todoist.Todoist;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
        Todoist.ProjectCreationRequest pcr = new Todoist.ProjectCreationRequest();
        pcr.name = eventName;
        pcr.parent_id = parent.id;
        Project project = todoist.createProject(pcr);

        System.out.println("project created: " + project.name + " (" + project.id + ")");
        if (requireTravel) {
            System.out.println("⚙️  Creating travel section...");
            Todoist.SectionCreationRequest scr = new Todoist.SectionCreationRequest("Travel", project.id);
            Todoist.Section section = todoist.createSection(scr);
            // If travel - Estimate budget, Ask for budget, Wait for approval, Book travel, Book hotel, Add calendar slot (day - 7)
            System.out.println("⚙️  Creating travelling tasks...");
            Todoist.TaskCreationRequest tcr = new Todoist.TaskCreationRequest();
            tcr.content = eventName + " - Estimate travel budget";
            tcr.project_id = project.id;
            tcr.section_id = section.id;
            todoist.addTask(tcr);

            tcr.content = eventName + " - Ask for budget";
            todoist.addTask(tcr);

            tcr.content = eventName + " - Wait for project approval";
            todoist.addTask(tcr);

            tcr.content = eventName + " - Book travel to " + location;
            tcr.description = "Start date: " + startDate + " - End date: " + endDate;
            todoist.addTask(tcr);

            tcr.content = eventName + " - Book hotel in " + location;
            todoist.addTask(tcr);

            tcr.content = eventName + " - Add calendar slot";
            tcr.description = null;
            tcr.due_string = "1 week before " + startDate;
            todoist.addTask(tcr);

            tcr.content = eventName + " - Expense Report";
            tcr.priority = 3;
            tcr.due_string = "3 days after " + endDate;
            todoist.addTask(tcr);

            System.out.println("⚙️  Travel section created!");

        }

        if (needSubmission) {
            System.out.println("⚙️  Creating CFP section...");
            Todoist.SectionCreationRequest scr = new Todoist.SectionCreationRequest("CFP", project.id);
            Todoist.Section section = todoist.createSection(scr);
            // If submission (CFP website + deadline) - Write abstract, Submit abstract, Save title/abstract, Wait for acceptance
            System.out.println("⚙️  Creating CFP tasks...");
            Todoist.TaskCreationRequest tcr = new Todoist.TaskCreationRequest();
            tcr.content = eventName + " - Write title and abstract";
            tcr.project_id = project.id;
            tcr.section_id = section.id;
            tcr.due_string = "1 day before " + submissionDeadline;
            todoist.addTask(tcr);

            tcr.content = eventName + " - Submit abstract and save it";
            tcr.description = cfp;
            todoist.addTask(tcr);

            tcr.content = eventName + " - Wait for notification";
            tcr.description = null;
            tcr.due_string = null;
            todoist.addTask(tcr);

            System.out.println("⚙️  CFP section created!");
        }

        System.out.println("⚙️  Creating Material section...");
        Todoist.SectionCreationRequest scr = new Todoist.SectionCreationRequest("Material", project.id);
        Todoist.Section section = todoist.createSection(scr);
        //  Slide title !!3, Slide audience !!3, Slide talk details !!3 , Slide call for action !!3, Outline !!2,
        // Slides parts A, B, C !!1, Outline demo !!2, Implement demo !!1, Share demo + Add link to slides
        System.out.println("⚙️  Creating Material tasks...");
        Todoist.TaskCreationRequest tcr = new Todoist.TaskCreationRequest();
        tcr.content = eventName + " - Create slide deck files and directory";
        tcr.project_id = project.id;
        tcr.section_id = section.id;
        tcr.priority = 3;
        todoist.addTask(tcr);

        tcr.priority = 2;
        tcr.content = eventName + " - Title slide";
        todoist.addTask(tcr);

        tcr.content = eventName + " - Audience slide";
        todoist.addTask(tcr);

        tcr.content = eventName + " - Talk details slide";
        todoist.addTask(tcr);

        tcr.priority = 3;
        tcr.content = eventName + " - Outline of the talk";
        todoist.addTask(tcr);

        tcr.content = eventName + " - Outline of the demo";
        todoist.addTask(tcr);

        tcr.priority = 2;
        tcr.content = eventName + " - Call for action slide";
        todoist.addTask(tcr);

        tcr.priority = 4;
        tcr.content = eventName + " - Slide part A";
        todoist.addTask(tcr);

        tcr.content = eventName + " - Slide part B";
        todoist.addTask(tcr);

        tcr.content = eventName + " - Slide part C";
        todoist.addTask(tcr);

        tcr.content = eventName + " - Implement demo";
        todoist.addTask(tcr);

        tcr.priority = 1;
        tcr.content = eventName + " - Share demo and add link to the slides";
        todoist.addTask(tcr);

        System.out.println("⚙️  Material section created!");

        System.out.println("⚙️  Creating Preparation section...");
        scr = new Todoist.SectionCreationRequest("Preparation", project.id);
        section = todoist.createSection(scr);

        System.out.println("⚙️  Creating preparation tasks...");
        tcr = new Todoist.TaskCreationRequest();
        tcr.content = eventName + " - Dry Run 1";
        tcr.project_id = project.id;
        tcr.section_id = section.id;
        tcr.priority = 4;
        tcr.due_string = "2 days before " + startDate;
        todoist.addTask(tcr);

        tcr.content = eventName + " - Dry Run 2";
        tcr.due_string = "1 days before " + startDate;
        todoist.addTask(tcr);

        tcr.priority = 1;
        if (endDate != null) {
            tcr.due_string = endDate;
        } else {
            tcr.due_string = startDate;
        }
        tcr.content = eventName + " - Share slides";
        todoist.addTask(tcr);

        System.out.println("\uD83D\uDE4C️  DONE!");
    }

    private Project getParentProject() {
        return backend.getProjects().stream().filter(p -> p.name.equalsIgnoreCase(parentProjectName)).findAny()
                .orElseThrow(() -> new RuntimeException("Unable to find the project " + parentProjectName));
    }

    private boolean readBoolean(Scanner sc) {
        String s = sc.nextLine().toLowerCase().trim();
        return s.equals("true") || s.equals("yes") || s.equals("y");
    }
}
