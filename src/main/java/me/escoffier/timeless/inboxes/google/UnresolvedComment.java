package me.escoffier.timeless.inboxes.google;

import com.google.api.services.drive.model.Comment;
import com.google.api.services.drive.model.File;
import me.escoffier.timeless.model.NewTaskRequest;

import java.util.Objects;

import static me.escoffier.timeless.helpers.DueDates.todayOrTomorrow;

public class UnresolvedComment {

    private final File document;
    private final Comment comment;

    public UnresolvedComment(File document, Comment comment) {
        this.document = document;
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnresolvedComment that = (UnresolvedComment) o;
        return document.getId().equals(that.document.getId()) && comment.getId().equals(that.comment.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(document.getId(), comment.getId());
    }

    public NewTaskRequest asNewTaskRequest() {
        NewTaskRequest t = new UnresolvedComment.UnresolvedCommentTaskRequest(content(), this);
        t.addLabels("timeless/drive");
        return t;
    }

    public String content() {
        return String
                .format("[Pending comment in %s (%s)](%s)", document.getName(), comment.getAnchor(), document.getWebViewLink());
    }

    private static class UnresolvedCommentTaskRequest extends NewTaskRequest {

        private final UnresolvedComment comment;

        public UnresolvedCommentTaskRequest(String content, UnresolvedComment comment) {
            super(content,
                    null,
                    todayOrTomorrow());
            this.comment = comment;
        }

        @Override
        public String getIdentifier() {
            return comment.comment.getId();
        }
    }
}
