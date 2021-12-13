package io.quarkus.activity.model.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.activity.model.Task;
import io.quarkus.activity.model.TaskType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class JsonToPullRequestTaskMapper extends BaseTaskMapper {

    public List<Map<String, Task>> processPullRequestsFromSearch(JsonObject dataJson, String type) {
        List<Map<String, Task>> allPullRequests = new ArrayList<>();
        JsonArray pullRequests = dataJson.getJsonObject(type).getJsonArray("nodes");
        for (int index = 0; index < pullRequests.size(); index++) {
            JsonObject pullRequestJson = pullRequests.getJsonObject(index);
            allPullRequests.add(extractPullRequestItem(pullRequestJson));
        }

        return allPullRequests;
    }

    public Map<String, Task> extractPullRequestItem(JsonObject pullRequestJson) {
        Map<String, Task> pullRequests = new HashMap<>();
        long repoId = extractRepoId(pullRequestJson.getJsonObject("repository").getString("id"));
        String assignee = pullRequestJson.getJsonObject("author").getString("login");
        Task pullRequest = new Task(
                TaskType.PULL_REQUEST,
                repoId,
                pullRequestJson.getJsonObject("repository").getString("nameWithOwner"),
                pullRequestJson.getInteger("number"),
                pullRequestJson.getString("title"),
                pullRequestJson.getString("url"),
                pullRequestJson.getInstant("createdAt"),
                pullRequestJson.getInstant("updatedAt"),
                pullRequestJson.getString("state"),
                Arrays.asList(assignee));
        pullRequest.setDraft(pullRequestJson.getBoolean("isDraft"));
        getLabels(pullRequestJson).forEach(pullRequest::addLabel);
        pullRequests.put(assignee, pullRequest);
        return pullRequests;
    }
}
