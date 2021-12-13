package io.quarkus.activity.model.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.activity.model.LinkedPullRequest;
import io.quarkus.activity.model.Task;
import io.quarkus.activity.model.TaskType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class JsonToIssueTaskMapper extends BaseTaskMapper {

    public List<Map<String, Task>> processIssuesFromSearch(JsonObject dataJson, String type, String defaultAssignee) {
        List<Map<String, Task>> allIssues = new ArrayList<>();
        JsonArray issues = dataJson.getJsonObject(type).getJsonArray("nodes");
        for (int index = 0; index < issues.size(); index++) {
            JsonObject issueJson = issues.getJsonObject(index);
            allIssues.add(extractIssueItem(issueJson, defaultAssignee));
        }

        return allIssues;
    }

    public Map<String, Task> extractIssueItem(JsonObject issueJson, String defaultAssignee) {
        Map<String, Task> issues = new HashMap<>();
        long repoId = extractRepoId(issueJson.getJsonObject("repository").getString("id"));
        Set<String> assignees = getAssigneesInIssue(issueJson);
        if (assignees.isEmpty()) {
            assignees = Set.of(defaultAssignee);
        }

        for (String assignee : assignees) {
            // Create issue
            Task task = new Task(
                    TaskType.ISSUE,
                    repoId,
                    issueJson.getJsonObject("repository").getString("nameWithOwner"),
                    issueJson.getInteger("number"),
                    issueJson.getString("title"),
                    issueJson.getString("url"),
                    issueJson.getInstant("createdAt"),
                    issueJson.getInstant("updatedAt"),
                    issueJson.getString("state"),
                    new ArrayList<>(assignees));

            // labels
            getLabels(issueJson).forEach(task::addLabel);

            // linked pull request and use the most recent updated
            task.getLinkedPullRequest().addAll(getLinkedPullRequest(issueJson));

            issues.put(assignee, task);
        }

        return issues;
    }

    private Set<LinkedPullRequest> getLinkedPullRequest(JsonObject objectJson) {
        Set<LinkedPullRequest> linkedPullRequests = new HashSet<>();
        if (objectJson.containsKey("timelineItems")) {
            JsonArray timelineItems = objectJson.getJsonObject("timelineItems").getJsonArray("nodes");
            for (int index = 0; index < timelineItems.size(); index++) {
                JsonObject linkedPullRequestJson = timelineItems.getJsonObject(index);
                JsonObject dataLinkedPullRequestJson = linkedPullRequestJson.getJsonObject("closer");
                if (dataLinkedPullRequestJson == null || !dataLinkedPullRequestJson.containsKey("number")) {
                    dataLinkedPullRequestJson = linkedPullRequestJson.getJsonObject("source");
                }

                if (dataLinkedPullRequestJson != null && dataLinkedPullRequestJson.containsKey("number")) {
                    LinkedPullRequest linkedPullRequest = new LinkedPullRequest(
                            dataLinkedPullRequestJson.getInteger("number"),
                            dataLinkedPullRequestJson.getString("url"),
                            dataLinkedPullRequestJson.getInstant("createdAt"),
                            dataLinkedPullRequestJson.getInstant("updatedAt"),
                            dataLinkedPullRequestJson.getBoolean("isDraft")
                    );

                    linkedPullRequests.add(linkedPullRequest);
                }
            }
        }

        return linkedPullRequests;
    }

    private Set<String> getAssigneesInIssue(JsonObject objectJson) {
        if (objectJson.containsKey("assignees")) {
            return objectJson.getJsonObject("assignees").getJsonArray("nodes")
                    .stream()
                    .map(json -> ((JsonObject) json).getString("login"))
                    .collect(Collectors.toSet());
        }

        return Collections.emptySet();
    }
}
