package me.escoffier.timeless.inboxes.github;

import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.Inbox;
import me.escoffier.timeless.model.NewTaskRequest;
import me.escoffier.timeless.model.Task;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.kohsuke.github.*;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@ApplicationScoped
public class GithubService implements Inbox {

    private static final Logger LOGGER = Logger.getLogger("Github");

    @Inject @RestClient GithubIssues githubIssues;

    @Inject @ConfigProperty(name = "github.projects") List<String> repositories;

    @Inject @ConfigProperty(name = "github.username") String username;

    private final List<Issue> issues = new ArrayList<>();
    private final List<Review> reviews = new ArrayList<>();
    private final List<GHPullRequest> followUps = new ArrayList<>();

    @PostConstruct
    public void fetch() throws IOException, InterruptedException {
        issues.clear();
        reviews.clear();
        followUps.clear();

        GitHub github = new GitHubBuilder().withOAuthToken(GithubIssues.token()).build();

        LOGGER.info("\uD83D\uDEB6  Retrieving Github Issues...");
        issues.addAll(githubIssues.getOpenIssuesAssignedToMe());
        LOGGER.infof("\uD83D\uDEB6  %d GitHub issues retrieved", issues.size());

        LOGGER.info("\uD83D\uDEB6  Retrieving Github PRs from selected projects...");
        List<GHPullRequest> prs = new CopyOnWriteArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(repositories.size());
        for (String repo : repositories) {
            pool.submit(() -> {
                try {
                    GHRepository repository = github.getRepository(repo);
                    prs.addAll(repository.getPullRequests(GHIssueState.OPEN));
                } catch (IOException e) {
                    LOGGER.error("☠️ Unable to review pull requests", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(1, TimeUnit.MINUTES);
        pool.shutdownNow();
        LOGGER.infof("\uD83D\uDEB6  %d PRs with pending reviews", reviews.size());
        checkForReviews(prs);
        checkForFollowUps(prs);
    }

    private void checkForFollowUps(List<GHPullRequest> prs) throws IOException {
        for (GHPullRequest pr : prs) {
            // Check requested reviews
            if (pr.getUser().getLogin().equals(username)  && ! pr.isDraft()) {
                followUps.add(pr);
            }
        }
    }

    private void checkForReviews(List<GHPullRequest> prs) throws IOException {
        for (GHPullRequest pr : prs) {
            // Check requested reviews
            boolean requested = isReviewRequested(pr, username);
            if (requested) {
                // Check if already done
                boolean reviewAlreadyDone = false;
                for (GHPullRequestReview review : pr.listReviews()) {
                    if (review.getUser().getName().equalsIgnoreCase(username)) {
                        reviewAlreadyDone = true;
                        break;
                    }
                }
                if (!reviewAlreadyDone) {
                    reviews.add(new Review(pr.getTitle(), pr.getRepository().getName(), pr.getNumber(),
                            pr.getHtmlUrl().toExternalForm()));
                }
            }
        }
    }

    private boolean isReviewRequested(GHPullRequest request, String name) throws IOException {
        for (GHUser requested : request.getRequestedReviewers()) {
            if (requested.getLogin().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Runnable> getPlan(Backend backend) {
        List<Task> existingIssues = backend.getMatchingTasks(this::isAGithubIssue);
        List<Task> existingReviewRequests = backend.getMatchingTasks(this::isAGithubReviewRequest);
        List<Task> existingFollowUps = backend.getMatchingTasks(this::isPRFollowUp);

        // 1 - If fetched contains an open issue without an associated task -> create new task
        // 2 - If fetched contains an open issue with an uncompleted associated task -> do nothing
        // 3 - If fetched contains a closed issue with an uncompleted associated task -> complete the task
        // 4 - If backend contains issue tasks (existingIssues) without an associated issue in fetched -> complete task

        List<Runnable> actions = new ArrayList<>();
        for (Issue issue : issues) {
            NewTaskRequest request = issue.asNewTaskRequest();
            Optional<Task> maybe = backend.getTaskMatchingRequest(request);

            if (!issue.isOpen() && maybe.isPresent()) {
                // Case 3
                actions.add(() -> backend.complete(maybe.get()));
            } else if (issue.isOpen() && maybe.isEmpty()) {
                // Case 1
                actions.add(() -> backend.create(request));
            } else if (!issue.isOpen() && maybe.isPresent()) {
                actions.add((() -> backend.complete(maybe.get())));
            }
        }

        for (Task task : existingIssues) {
            Optional<Issue> thread = issues.stream()
                    .filter(s -> task.content.startsWith("[" + s.getTaskName() + "]"))
                    .findFirst();
            if (thread.isEmpty()) {
                // 4 - complete the task
                actions.add(() -> backend.complete(task));
            }
        }

        // 1 - If fetched contains an open PR with a requested review without an associated task -> create new task
        // 2 - If fetched contains an open PR with a requested review with an uncompleted associated task -> do nothing
        // 3 - If backend contains issue tasks (existingReviewRequests) without an associated PR in fetched -> complete task

        for (Review review : reviews) {
            NewTaskRequest request = review.asNewTaskRequest();
            Optional<Task> maybe = backend.getTaskMatchingRequest(request);

            if (maybe.isEmpty()) {
                // Case 1 - we have a requested review but no associated tasks
                actions.add(() -> backend.create(request));
            }
        }

        for (Task task : existingReviewRequests) {
            Optional<Review> r = reviews.stream()
                    .filter(s -> task.content.startsWith("[" + s.getTaskName() + "]"))
                    .findFirst();
            if (r.isEmpty()) {
                // 3 - complete the task
                actions.add(() -> backend.complete(task));
            }
        }

        // 1 - If fetched contains a PR open by me without an associated task -> create new task
        // 2 - If fetched contains a PR open by me with an uncompleted associated task -> do nothing
        // 3 - If backend contains issue tasks (existingFollowUps) without an associated PR in fetched -> complete task

        for (GHPullRequest pr : followUps) {
            NewTaskRequest request = createFollowUpTaskRequestForPr(pr);
            Optional<Task> maybe = backend.getTaskMatchingRequest(request);

            if (maybe.isEmpty()) {
                // Case 1 - we have a requested review but no associated tasks
                actions.add(() -> backend.create(request));
            }
        }

        for (Task task : existingFollowUps) {
            Optional<NewTaskRequest> r = followUps.stream()
                    .map(this::createFollowUpTaskRequestForPr)
                    .filter(s -> task.content.startsWith(s.content))
                    .findFirst();
            if (r.isEmpty()) {
                // 3 - complete the task
                actions.add(() -> backend.complete(task));
            }
        }

        return actions;
    }

    private NewTaskRequest createFollowUpTaskRequestForPr(GHPullRequest pr) {
        String content = "Follow Up PR " + pr.getTitle();
        return new NewTaskRequest(
                content,
                pr.getHtmlUrl().toExternalForm(),
                null,
                null
        );
    }

    private boolean isAGithubIssue(Task task) {
        return task.content.startsWith("[Fix ") && task.content.contains("](https://github.com/");
    }

    private boolean isAGithubReviewRequest(Task task) {
        return task.content.startsWith("[Review PR ") && task.content.contains("](https://github.com/");
    }

    private boolean isPRFollowUp(Task task) {
        return task.content.startsWith("[Follow Up PR ") && task.content.contains("](https://github.com/");
    }
}
