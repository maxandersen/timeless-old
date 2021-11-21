package me.escoffier.timeless.inboxes.google;

import com.google.api.services.gmail.model.Message;
import me.escoffier.timeless.model.NewTaskRequest;

import static me.escoffier.timeless.helpers.DueDates.todayOrTomorrow;

public class StarredThread {

    private final String threadId;
    private final String subject;
    private final String sender;
    private final String snippet;
    private final Account account;
    private final Message message;

    public StarredThread(Account account, String threadId, Message message, String subject, String snippet,
            String sender) {
        this.account = account;
        this.message = message;
        this.threadId = threadId;
        this.subject = subject;
        this.sender = sender;
        this.snippet = snippet;
    }

    public String thread() {
        return threadId;
    }

    public String subject() {
        return subject;
    }

    public String sender() {
        return sender;
    }

    public String snippet() {
        return snippet;
    }

    public Account account() {
        return account;
    }

    public Message message() {
        return message;
    }

    public NewTaskRequest asNewTaskRequest() {
        return new MailTaskRequest(this);
    }

    String link() {
        int inboxId = 0;
        if (account.name().equalsIgnoreCase("redhat")) {
            inboxId = 1;
        }

        return String
                .format("https://mail.google.com/mail/u/%d/#inbox/%s", inboxId,
                        message.getId()
                );
    }

    public String content() {

        return String
                .format("[%s](%s)", subject, link()
                );
    }

    private static class MailTaskRequest extends NewTaskRequest {

        private final StarredThread thread;

        public MailTaskRequest(StarredThread thread) {
            super(thread.subject(),
                    thread.link(),
                    null,
                    todayOrTomorrow());
            this.thread = thread;
            this.addLabels("timeless/gmail");
        }

        @Override
        public String getIdentifier() {
            return thread.message.getId();
        }
    }
}
