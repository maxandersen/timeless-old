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
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Account {

    private static final String APPLICATION_NAME = "Time Management";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_MODIFY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private final String name;
    private final String tokenDirectory;
    private final Gmail service;
    private final int port;

    Account(String name, String tokenDirectory, int port) {
        try {
            this.name = name;
            this.tokenDirectory = tokenDirectory;
            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            this.port = port;
            this.service = new Gmail.Builder(transport, JSON_FACTORY, getCredentials(transport))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize the GMAIL service", e);
        }
    }

    public Collection<StarredThread> getStarredMessages() throws IOException {
        Map<String, StarredThread> threads = new LinkedHashMap<>();
        String user = "me";
        ListMessagesResponse list = service().users().messages().list(user)
                .setQ("is:starred")
                .setUserId("me")
                .setMaxResults(1000L)
                .execute();
        List<Message> messages = list.getMessages();
        if (messages == null) {
            return Collections.emptyList();
        }
        for (Message m : messages) {
            Message message = service().users().messages().get("me", m.getId())
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

    public Gmail service() {
        return service;
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
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, clientSecrets, SCOPES)
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
        service.users().messages().modify("me", starred.message().getId(),
                new ModifyMessageRequest().setRemoveLabelIds(Collections.singletonList("STARRED"))).execute();
        service.users().threads().modify("me", starred.thread(),
                new ModifyThreadRequest().setRemoveLabelIds(Collections.singletonList("STARRED"))).execute();
    }
}
