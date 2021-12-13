package io.quarkus.activity.model;

import java.time.Instant;
import java.util.Objects;

public class LinkedPullRequest {
    private final int id;
    private final String url;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final boolean draft;

    public LinkedPullRequest(int id, String url, Instant createdAt, Instant updatedAt, boolean draft) {
        this.id = id;
        this.url = url;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.draft = draft;
    }

    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDraft() {
        return draft;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LinkedPullRequest that = (LinkedPullRequest) o;
        return id == that.id && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url);
    }
}
