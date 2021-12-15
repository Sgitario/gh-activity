package io.quarkus.activity.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.activity.model.Task;

@ApplicationScoped
public class DescriptionEpicIssuesLoader extends BaseEpicIssuesLoader {

    private static final String ISSUES_START = "{issues}";
    private static final String ISSUES_END = "{/issues}";
    private static final String ISSUE_START = "- ";

    public List<Task> getLinkedIssues(Task epicTask, String defaultAssignee) throws IOException {
        if (epicTask.getDescription().isEmpty()) {
            return Collections.emptyList();
        }

        String list = StringUtils.substringBetween(epicTask.getDescription(), ISSUES_START, ISSUES_END);
        if (StringUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        String[] issuesUrl = list.split(ISSUE_START);
        long repoCounter = 0;
        Map<String, Long> repositoryNameById = new HashMap<>();
        Map<Long, GitHubWeeklyManagementReportService.RepositoryQuery> issuesByRepository = new HashMap<>();
        for (String issueUrl : issuesUrl) {
            if (StringUtils.isBlank(issueUrl)) {
                continue;
            }
            // Example https://github.com/quarkusio/quarkus/issues/22055
            String[] parts = issueUrl.trim().split("/");
            String repoName = parts[parts.length - 4]; // quarkusio
            Long repoId = repositoryNameById.get(repoName);
            if (repoId == null) {
                repoId = ++repoCounter;
                repositoryNameById.put(repoName, repoId);
            }

            GitHubWeeklyManagementReportService.RepositoryQuery repositoryQuery = issuesByRepository.get(repoId);
            if (repositoryQuery == null) {
                repositoryQuery = new GitHubWeeklyManagementReportService.RepositoryQuery();
                repositoryQuery.repoId = repoId;
                repositoryQuery.owner = repoName;
                repositoryQuery.name = parts[parts.length - 3]; // quarkus
                repositoryQuery.issues = new ArrayList<>();
                issuesByRepository.put(repoId, repositoryQuery);
            }

            repositoryQuery.issues.add(Integer.parseInt(parts[parts.length - 1])); // 22055
        }

        return getTasksUsingGitHub(defaultAssignee, issuesByRepository);
    }
}
