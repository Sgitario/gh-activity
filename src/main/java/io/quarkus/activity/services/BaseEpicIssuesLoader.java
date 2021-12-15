package io.quarkus.activity.services;

import static io.quarkus.activity.graphql.GraphQLUtils.handleErrors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.activity.graphql.GraphQLClient;
import io.quarkus.activity.model.Task;
import io.quarkus.activity.model.mapping.JsonToIssueTaskMapper;
import io.quarkus.activity.model.mapping.JsonToPullRequestTaskMapper;
import io.vertx.core.json.JsonObject;

public abstract class BaseEpicIssuesLoader {

    @ConfigProperty(name = "activity.token")
    String token;

    @Inject
    @RestClient
    GraphQLClient graphQLClient;

    @Inject
    JsonToIssueTaskMapper jsonToIssueTaskMapper;

    @Inject
    JsonToPullRequestTaskMapper jsonToPullRequestTaskMapper;

    protected List<Task> getTasksUsingGitHub(String defaultAssignee,
            Map<Long, GitHubWeeklyManagementReportService.RepositoryQuery> issuesByRepository) throws IOException {
        List<Task> linkedTasks = new ArrayList<>();
        // Retrieve issues and pull requests by user
        String query = GitHubWeeklyManagementReportService.Templates.issues(issuesByRepository.values()).render();
        JsonObject response = graphQLClient.graphql("Bearer " + token, new JsonObject().put("query", query));
        handleErrors(response);
        JsonObject dataJson = response.getJsonObject("data");

        for (GitHubWeeklyManagementReportService.RepositoryQuery repository : issuesByRepository.values()) {
            JsonObject repoJson = dataJson.getJsonObject("repo_" + repository.getRepoId());
            for (Integer issue : repository.issues) {
                Map<String, Task> tasks;
                JsonObject issueJson = repoJson.getJsonObject("issue_" + issue);
                if (issueJson != null) {
                    tasks = jsonToIssueTaskMapper.extractIssueItem(issueJson, defaultAssignee);
                } else {
                    tasks = jsonToPullRequestTaskMapper.extractPullRequestItem(repoJson.getJsonObject("pr_" + issue));
                }

                for (Map.Entry<String, Task> task : tasks.entrySet()) {
                    if (!linkedTasks.contains(task.getValue())) {
                        linkedTasks.add(task.getValue());
                    }
                }
            }
        }

        return linkedTasks;
    }
}
