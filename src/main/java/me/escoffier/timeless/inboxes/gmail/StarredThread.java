package me.escoffier.timeless.inboxes.gmail;

import com.google.api.services.gmail.model.Message;
import me.escoffier.timeless.model.NewTaskRequest;

import java.util.Calendar;

import static me.escoffier.timeless.helpers.TodayOrTomorrow.todayOrTomorrow;

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
        return new MailTaskRequest(content(), this);
    }

    public String content() {
        int inboxId = 0;
        if (account.name().equalsIgnoreCase("redhat")) {
            inboxId = 1;
        }

        return String
                .format("[%s](https://mail.google.com/mail/u/%d/#inbox/%s)", subject, inboxId,
                    message.getId()
                );
    }

    private static class MailTaskRequest extends NewTaskRequest {

        private final StarredThread thread;

        public MailTaskRequest(String content, StarredThread thread) {
            super(content,
                    null,
                    todayOrTomorrow());
            this.thread = thread;
        }

        @Override
        public String getIdentifier() {
            return thread.message.getId();
        }
    }
}
