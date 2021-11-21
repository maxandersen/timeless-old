package me.escoffier.timeless.inboxes.google;

import com.google.api.services.calendar.model.Event;
import me.escoffier.timeless.model.NewTaskRequest;

public class Meeting {

    private final String title;
    private final String date;
    private final Event item;

    public Meeting(Event item, String title, String date) {
        this.item = item;
        this.title = title;
        this.date = date;
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
        String d = date;
        if (date.contains("T")) {
            d = d.substring(0, date.indexOf("T"));
        }
        return String.format("[Prepare meeting '%s (%s)'](%s)", title, d, item.getHtmlLink());
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
            addLabels("timeless/gcal");
        }

        @Override
        public String getIdentifier() {
            return meeting.content() + "-" + meeting.getDate();
        }
    }
}
