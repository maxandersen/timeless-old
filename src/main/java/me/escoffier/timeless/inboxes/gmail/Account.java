package me.escoffier.timeless.inboxes.gmail;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.api.services.calendar.Calendar;
import org.jboss.logging.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Account {

    private static final String APPLICATION_NAME = "Time Management";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final Logger LOGGER = Logger.getLogger("Account");


    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> GMAIL_SCOPES = Collections.singletonList(GmailScopes.GMAIL_MODIFY);

    /**
     * Read events.
     */
    private static final List<String> CALENDAR_SCOPES = Collections.singletonList(CalendarScopes.CALENDAR_EVENTS_READONLY);

    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private final String name;
    private final String tokenDirectory;
    private final Gmail gmail;
    private final int port;
    private final Calendar calendar;

    Account(String name, String tokenDirectory, int port) {
        try {
            this.name = name;
            this.tokenDirectory = tokenDirectory;
            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            this.port = port;
            this.gmail = new Gmail.Builder(transport, JSON_FACTORY, getCredentials(transport))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            this.calendar = new Calendar.Builder(transport, JSON_FACTORY, getCredentials(transport))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize the GMAIL service", e);
        }
    }

    public Collection<Meeting> getMeetings() {
        try {
            LocalDate now = LocalDate.now();
            LocalDate week = now.plus(7, ChronoUnit.DAYS);
            List<Event> items = calendar().events().list("primary")
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .setTimeMin(new DateTime(new Date()))
                    .setTimeMax(new DateTime(week.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()))
                    .execute().getItems();
            List<Meeting> meetings = new ArrayList<>();
            for (Event item : items) {
                if (isCall(item)  && isAccepted(item)) {
                    Meeting meeting = new Meeting(this, item.getSummary(), item.getStart().getDateTime().toStringRfc3339());
                    meetings.add(meeting);
                } else {
                    if (! isAccepted(item)) {
                        LOGGER.infof("Ignoring meeting %s - Event has not been accepted", item.getSummary());
                    }
                }
            }
            return meetings;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to get calendar events", e);
        }
    }

    private boolean isCall(Event item) {
        return item.getHangoutLink() != null  || item.getLocation() != null  && item.getLocation().contains("meet.google.com");
    }

    private boolean isAccepted(Event item) {
        if (item.getCreator().isSelf()  && item.getStatus().equalsIgnoreCase("confirmed")) {
            return true;
        }
        List<EventAttendee> attendees = item.getAttendees();
        if (attendees == null) {
            return true;
        }
        for (EventAttendee attendee : attendees) {
            if (attendee.isSelf()) {
                return attendee.getResponseStatus().equalsIgnoreCase("accepted");
            }
        }
        return false;
    }


    public Collection<StarredThread> getStarredMessages() throws IOException {
        Map<String, StarredThread> threads = new LinkedHashMap<>();
        String user = "me";
        ListMessagesResponse list = gmail().users().messages().list(user)
                .setQ("is:starred")
                .setUserId("me")
                .setMaxResults(1000L)
                .execute();
        List<Message> messages = list.getMessages();
        if (messages == null) {
            return Collections.emptyList();
        }
        for (Message m : messages) {
            Message message = gmail().users().messages().get("me", m.getId())
                    .setFormat("full")
                    .execute();
            String threadId = message.getThreadId();
            if (! threads.containsKey(threadId)) {
                String subject = subject(message);
                threads.put(threadId, new StarredThread(this, threadId, m, subject, message.getSnippet(), from(message)));
            }
        }
        return threads.values();
    }

    public String name() {
        return name;
    }

    public Gmail gmail() {
        return gmail;
    }

    public Calendar calendar() {
        return calendar;
    }

    /**
     * Creates an authorized Credential object.
     *
     * @param transport The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private Credential getCredentials(NetHttpTransport transport) throws IOException {
        // Load client secrets.
        InputStream in = GmailService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        List<String> scopes = new ArrayList<>();
        scopes.addAll(GMAIL_SCOPES);
        scopes.addAll(CALENDAR_SCOPES);
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, clientSecrets, scopes)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokenDirectory)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(port).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    static String subject(Message message) {
        return message.getPayload().getHeaders().stream()
                .filter(h -> h.getName().equalsIgnoreCase("Subject"))
                .map(MessagePartHeader::getValue)
                .findAny()
                .orElse(message.getSnippet());
    }

    static String from(Message message) {
        return message.getPayload().getHeaders().stream()
                .filter(h -> h.getName().equalsIgnoreCase("From"))
                .map(MessagePartHeader::getValue)
                .findAny()
                .orElse("unknown");
    }

    public void unflag(StarredThread starred) throws IOException {
        gmail.users().messages().modify("me", starred.message().getId(),
                new ModifyMessageRequest().setRemoveLabelIds(Collections.singletonList("STARRED"))).execute();
        gmail.users().threads().modify("me", starred.thread(),
                new ModifyThreadRequest().setRemoveLabelIds(Collections.singletonList("STARRED"))).execute();
    }
}
