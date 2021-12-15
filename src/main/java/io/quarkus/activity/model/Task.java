package io.quarkus.activity.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Task {
    private final long repoId;
    private final String repoName;
    private final int id;
    private final String name;
    private final String url;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final TaskStatus state;
    private final Set<String> labels = new HashSet<>();
    private final TaskType type;
    private final List<String> assignees;
    private Set<LinkedPullRequest> linkedPullRequest = new HashSet<>();
    private String description;
    private boolean draft = false;

    public Task(TaskType type, long repoId, String repoName, int id, String name, String url, Instant createdAt,
            Instant updatedAt, String state, List<String> assignees) {
        this.type = type;
        this.repoId = repoId;
        this.repoName = repoName;
        this.id = id;
        this.name = name;
        this.url = url;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.state = TaskStatus.from(state);
        this.assignees = assignees;
    }

    public TaskType getType() {
        return type;
    }

    public long getRepoId() {
        return repoId;
    }

    public String getRepoName() {
        return repoName;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public TaskStatus getState() {
        return state;
    }

    public String getUrl() {
        return url;
    }

    public Set<String> getLabels() {
        return labels;
    }

    public Set<LinkedPullRequest> getLinkedPullRequest() {
        return linkedPullRequest;
    }

    public List<String> getAssignees() {
        return assignees;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDraft(boolean draft) {
        this.draft = draft;
    }

    public void addLabel(String label) {
        labels.add(label);
    }

    public boolean isClosed() {
        return state == TaskStatus.FINISHED;
    }

    public boolean isLinkedPullRequestFound(String url) {
        return linkedPullRequest.stream().anyMatch(pr -> pr.getUrl().equals(url));
    }

    public String getStatus() {
        if (isClosed()) {
            return state.toString();
        }

        if (type == TaskType.ISSUE && linkedPullRequest.stream().anyMatch(pr -> !pr.isDraft())) {
            return "PR submitted for review";
        }

        if (type == TaskType.PULL_REQUEST) {
            if (draft) {
                return "PR in progress";
            } else {
                return "PR submitted for review";
            }
        }

        return state.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Task task = (Task) o;
        return repoId == task.repoId && id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(repoId, id);
    }
}
