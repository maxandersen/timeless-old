package me.escoffier.timeless.inboxes.google;

import com.google.api.services.gmail.model.*;
import jakarta.annotation.PostConstruct;
import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.Inbox;
import me.escoffier.timeless.model.NewTaskRequest;
import me.escoffier.timeless.model.Task;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

@ApplicationScoped
public class GMailService implements Inbox {

    private List<StarredThread> fetched;

    @Inject
    Logger logger;
    @Inject
    GoogleAccounts accounts;

    @Override
    public List<Runnable> getPlan(Backend backend) {
        if (fetched == null) {
            fetch();
        }

        List<Task> existingEmailTasks = backend.getMatchingTasks(this::isEmail);


        // 1 - If fetched contains an email without an associated task -> create new task
        // 2 - If fetched contains an email with an uncompleted associated task -> do nothing
        // 3 - If fetched contains an email with a completed associated task -> remove star (cannot be implemented as you cannot retrieve completed tasks)
        // 4 - If backend contains "email" tasks (existingEmailTasks) without an associated email in fetched -> complete task

        List<Runnable> actions = new ArrayList<>();
        for (StarredThread starred : fetched) {
            NewTaskRequest request = starred.asNewTaskRequest();
            Optional<Task> maybe = backend.getTaskMatchingRequest(request);
            if (maybe.isEmpty()) {
                // Case 1
                actions.add(() -> backend.create(request));
                continue;
            }
            Task task = maybe.get();
            if (task.isCompleted()) {
                // Case 3
                actions.add(() -> unflag(starred));
            }
        }


        for (Task task : existingEmailTasks) {
            Optional<StarredThread> thread = fetched.stream().filter(s -> s.content().equalsIgnoreCase(task.content)).findFirst();
            if (thread.isEmpty()) {
                // 4 - complete the task
                actions.add(() -> backend.complete(task));
            }
        }

        return actions;
    }

    private boolean isEmail(Task task) {
        return task.content.contains("](https://mail.google.com/mail/u/");
    }

    @PostConstruct
    public void fetch() {
        logger.info("\uD83D\uDEB6  Retrieving starred emails from Gmail...");
        List<StarredThread> messages = new ArrayList<>();
        for (Account account : accounts.accounts().values()) {
            try {
                messages.addAll(getStarredMessages(account));
            } catch (IOException e) {
                throw new IllegalStateException("\uD83D\uDC7F Unable to retrieve messages from Gmail for account " + account.email(), e);
            }
        }
        fetched = new ArrayList<>(messages);
        logger.infof("\uD83D\uDEB6  %d starred emails retrieved", fetched.size());
    }

    public Collection<StarredThread> getStarredMessages(Account account) throws IOException {
        Map<String, StarredThread> threads = new LinkedHashMap<>();
        String user = "me";
        ListMessagesResponse list = account.gmail().users().messages().list(user)
                .setQ("is:starred")
                .setUserId("me")
                .setMaxResults(1000L)
                .execute();
        List<Message> messages = list.getMessages();
        if (messages == null) {
            return Collections.emptyList();
        }
        for (Message m : messages) {
            Message message = account.gmail().users().messages().get("me", m.getId())
                    .setFormat("full")
                    .execute();
            String threadId = message.getThreadId();
            if (!threads.containsKey(threadId)) {
                String subject = subject(message);
                threads.put(threadId, new StarredThread(account, threadId, m, subject, message.getSnippet(), from(message)));
            }
        }
        return threads.values();
    }

    private String subject(Message message) {
        return message.getPayload().getHeaders().stream()
                .filter(h -> h.getName().equalsIgnoreCase("Subject"))
                .map(MessagePartHeader::getValue)
                .findAny()
                .orElse(message.getSnippet());
    }

    private String from(Message message) {
        return message.getPayload().getHeaders().stream()
                .filter(h -> h.getName().equalsIgnoreCase("From"))
                .map(MessagePartHeader::getValue)
                .findAny()
                .orElse("unknown");
    }

    private void unflag(StarredThread starred) {
        try {
            starred.account().gmail().users().messages().modify("me", starred.message().getId(),
                    new ModifyMessageRequest().setRemoveLabelIds(Collections.singletonList("STARRED"))).execute();
            starred.account().gmail().users().threads().modify("me", starred.thread(),
                    new ModifyThreadRequest().setRemoveLabelIds(Collections.singletonList("STARRED"))).execute();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to unstart email " + starred.link(), e);
        }
    }

}
