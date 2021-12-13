package io.quarkus.activity.model;

import java.time.Instant;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class OpenPullRequest {
    public Instant created;
    public String title;
    public String url;
    public String shortUrl;
    public String author;

    public OpenPullRequest(Instant created, String title, String url, String author) {
        this.created = created;
        this.title = title;
        this.url = url;
        this.author = author;
        this.shortUrl = url.substring(url.lastIndexOf("/") + 1);
    }

    @Override
    public String toString() {
        return "PullRequestWithReviewers{" +
                "created=" + created +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", author='" + author + '\'' +
                '}';
    }
}
