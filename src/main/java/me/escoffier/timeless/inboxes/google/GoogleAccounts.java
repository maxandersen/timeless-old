package me.escoffier.timeless.inboxes.google;

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
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import jakarta.annotation.PostConstruct;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.*;
import java.util.*;

@ApplicationScoped
public class GoogleAccounts {

    private static final String APPLICATION_NAME = "timeless";
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

    private static final List<String> DRIVE_SCOPES = Arrays.asList(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE, DriveScopes.DRIVE_METADATA);

    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    @ConfigProperty(name = "google.accounts")
    List<String> names;
    @ConfigProperty(name = "google.token-directory-prefix", defaultValue = "token")
    String tokenDirectoryPrefix;

    @Inject
    Logger logger;

    private Map<String, Account> accounts = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            var transport = GoogleNetHttpTransport.newTrustedTransport();
            var port = 8888;
            for (String name : names) {
                logger.infof("\uD83D\uDEB6  Setting up %s Google account", name);
                var credentials = getCredentials(name, transport, port);
                port++;

                var gmail = new Gmail.Builder(transport, JSON_FACTORY, credentials)
                        .setApplicationName(APPLICATION_NAME)
                        .build();
                var email = gmail.users().getProfile("me").execute().getEmailAddress();
                var calendar = new Calendar.Builder(transport, JSON_FACTORY, credentials)
                        .setApplicationName(APPLICATION_NAME)
                        .build();
                var drive = new Drive.Builder(transport, JSON_FACTORY, credentials)
                        .setApplicationName(APPLICATION_NAME)
                        .build();
                var account = new Account(name, email, gmail, calendar, drive);
                logger.infof("\uD83D\uDEB6  Google account configured for %s (%s)", name, email);
                accounts.put(name, account);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize the Google service", e);
        }
    }


    /**
     * Creates an authorized Credential object.
     *
     * @param transport The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private Credential getCredentials(String name, NetHttpTransport transport, int port) throws IOException {
        // Load client secrets.
        var in = GoogleAccounts.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            // total hack to look up in current dir if not found in project which is just wrong.
            // todo: make this location actually relative.
            try {
                in = new FileInputStream(System.getProperty("user.dir") + CREDENTIALS_FILE_PATH);
            } catch (FileNotFoundException ffe) {
                throw new FileNotFoundException("Resource nor file not found: " + CREDENTIALS_FILE_PATH);
            }
        }

        var clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        List<String> scopes = new ArrayList<>();
        scopes.addAll(GMAIL_SCOPES);
        scopes.addAll(CALENDAR_SCOPES);
        scopes.addAll(DRIVE_SCOPES);
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, clientSecrets, scopes)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokenDirectoryPrefix + "-" + name)))
                .setAccessType("offline")
                .build();

        var receiver = new LocalServerReceiver.Builder().setPort(port).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public Map<String, Account> accounts() {
        return accounts;
    }
}
