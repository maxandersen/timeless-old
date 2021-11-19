package me.escoffier.timeless.inboxes.gmail;

import me.escoffier.timeless.model.NewTaskRequest;
import me.escoffier.timeless.model.Task;

import java.util.Optional;

public class Meeting {

    private final Account account;
    private final String title;
    private final String date;

    public Meeting(Account account, String title, String date) {
        this.account = account;
        this.title = title;
        this.date = date;
    }

    public Account getAccount() {
        return account;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        return date;
    }

    public NewTaskRequest asNewTaskRequest(String projectIfAny) {
        return new Meeting.MeetingTaskRequest(this, projectIfAny);
    }


    public String content() {
        return String.format("Prepare meeting '%s'", title);
    }

    private static class MeetingTaskRequest extends NewTaskRequest {

        private final Meeting meeting;

        public MeetingTaskRequest(Meeting meeting, String project) {
            super(meeting.content(),
                    project,
                    meeting.date);
            this.meeting = meeting;
            setPriority(2);
            addLabels("meeting");
        }

        @Override
        public String getIdentifier() {
            return meeting.content() + "-" + meeting.getDate();
        }
    }
}
