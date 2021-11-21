package me.escoffier.timeless.inboxes.pocket;

import com.google.common.base.Strings;
import me.escoffier.timeless.model.NewTaskRequest;

import java.util.Map;

public class Item {

    public static final String READING_LIST_PROJECT = "Reading List";
    private long item_id;

    private long resolved_id;

    private String given_url;

    private String given_title;

    private String favorite;

    private int status;

    private int word_count;

    private long time_read;

    private long time_added;

    private long time_updated;

    private long time_favorited;

    private int sort_id;

    private String resolved_title;

    private String resolved_url;

    private String excerpt;

    private int is_article;

    private int is_index;

    private int has_video;

    private int has_image;

    private String amp_url;

    private Map<String, Object> tags;

    private Map<String, Object> authors;

    public long getItem_id() {
        return item_id;
    }

    public Item setItem_id(long item_id) {
        this.item_id = item_id;
        return this;
    }

    public long getResolved_id() {
        return resolved_id;
    }

    public Item setResolved_id(long resolved_id) {
        this.resolved_id = resolved_id;
        return this;
    }

    public String getGiven_url() {
        return given_url;
    }

    public Item setGiven_url(String given_url) {
        this.given_url = given_url;
        return this;
    }

    public String getGiven_title() {
        return given_title;
    }

    public Item setGiven_title(String given_title) {
        this.given_title = given_title;
        return this;
    }

    public boolean isFavorite() {
        return "1".equals(favorite);
    }

    public Item setFavorite(String favorite) {
        this.favorite = favorite;
        return this;
    }

    public String getFavorite() {
        return favorite;
    }

    public int getStatus() {
        return status;
    }

    public Item setStatus(int status) {
        this.status = status;
        return this;
    }

    public int getWord_count() {
        return word_count;
    }

    public Item setWord_count(int word_count) {
        this.word_count = word_count;
        return this;
    }

    public boolean isRead() {
        return status == 1;
    }

    public long getTime_added() {
        return time_added;
    }

    public Item setTime_added(long time_added) {
        this.time_added = time_added;
        return this;
    }

    public long getTime_updated() {
        return time_updated;
    }

    public Item setTime_updated(long time_updated) {
        this.time_updated = time_updated;
        return this;
    }

    public int getSort_id() {
        return sort_id;
    }

    public Item setSort_id(int sort_id) {
        this.sort_id = sort_id;
        return this;
    }

    public String getResolved_title() {
        return resolved_title;
    }

    public Item setResolved_title(String resolved_title) {
        this.resolved_title = resolved_title;
        return this;
    }

    public String getResolved_url() {
        return resolved_url;
    }

    public Item setResolved_url(String resolved_url) {
        this.resolved_url = resolved_url;
        return this;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public Item setExcerpt(String excerpt) {
        this.excerpt = excerpt;
        return this;
    }

    public int getIs_article() {
        return is_article;
    }

    public Item setIs_article(int is_article) {
        this.is_article = is_article;
        return this;
    }

    public int getIs_index() {
        return is_index;
    }

    public Item setIs_index(int is_index) {
        this.is_index = is_index;
        return this;
    }

    public int getHas_video() {
        return has_video;
    }

    public Item setHas_video(int has_video) {
        this.has_video = has_video;
        return this;
    }

    public int getHas_image() {
        return has_image;
    }

    public Item setHas_image(int has_image) {
        this.has_image = has_image;
        return this;
    }

    public long getTime_read() {
        return time_read;
    }

    public Item setTime_read(long time_read) {
        this.time_read = time_read;
        return this;
    }

    public long getTime_favorited() {
        return time_favorited;
    }

    public Item setTime_favorited(long time_favorited) {
        this.time_favorited = time_favorited;
        return this;
    }

    public String getAmp_url() {
        return amp_url;
    }

    public Item setAmp_url(String amp_url) {
        this.amp_url = amp_url;
        return this;
    }

    private String getTitle() {
        String title = getResolved_title();
        if (Strings.isNullOrEmpty(title)) {
            title = getGiven_title();
        }
        return title.trim();
    }

    public NewTaskRequest asNewTaskRequest() {

        var x = new NewTaskRequest(
                getTaskTitle(),
                "https://app.getpocket.com/read/" + getItem_id(),
                READING_LIST_PROJECT,
                null
        );
        x.addLabels("timeless/pocket");

        return x;
    }

    public String getTaskTitle() {
        String t = getTitle();
        if (t.isBlank()) {
            t = getGiven_url();
        }
        String title = "To read - " + t;
        if (has_video == 1) {
            title = "To watch - " + t;
        }
        return title;
    }

    public Map<String, Object> getTags() {
        return tags;
    }

    public Item setTags(Map<String, Object> tags) {
        this.tags = tags;
        return this;
    }

    public Map<String, Object> getAuthors() {
        return authors;
    }

    public Item setAuthors(Map<String, Object> authors) {
        this.authors = authors;
        return this;
    }

}
