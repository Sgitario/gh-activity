package io.quarkus.activity.services;

import static io.quarkus.activity.graphql.GraphQLUtils.DATE_SEARCH_FORMATTER;
import static io.quarkus.activity.graphql.GraphQLUtils.handleErrors;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.activity.graphql.GraphQLClient;
import io.quarkus.activity.model.ProjectBasedOnTasksByUser;
import io.quarkus.activity.model.Task;
import io.quarkus.activity.model.TaskType;
import io.quarkus.activity.model.WeeklyTeamReport;
import io.quarkus.activity.model.mapping.JsonToIssueTaskMapper;
import io.quarkus.activity.model.mapping.JsonToPullRequestTaskMapper;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class GitHubWeeklyTeamReportService {

    private static final String ISSUES_ASSIGNED = "_issues_assigned";
    private static final String PULL_REQUESTS_CREATED = "_prs_created";

    private static final String PROJECT_LABEL = "project/";

    @ConfigProperty(name = "activity.token")
    String token;

    @ConfigProperty(name = "activity.logins")
    List<String> logins;

    @Inject
    @RestClient
    GraphQLClient graphQLClient;

    @Inject
    ProjectMappingService projectMappingService;

    @Inject
    JsonToPullRequestTaskMapper jsonToPullRequestTaskMapper;

    @Inject
    JsonToIssueTaskMapper jsonToIssueTaskMapper;

    @CheckedTemplate
    private static class Templates {
        public static native TemplateInstance weeklyReport(Collection<String> logins, String timeWindow);
    }

    public WeeklyTeamReport getWeeklyReport(LocalDate reportStart, String selectedLogin) throws IOException {
        WeeklyTeamReport report = new WeeklyTeamReport();
        report.projects = new LinkedList<>();
        report.selectedLogin = selectedLogin;

        LocalDate weekStartDate = reportStart.with(DayOfWeek.MONDAY);
        LocalDate weekEndDate = reportStart.with(DayOfWeek.SUNDAY);
        report.week = DATE_SEARCH_FORMATTER.format(weekStartDate);

        List<String> selectedLogins = getLogins(selectedLogin);

        // Init all projects
        Map<String, ProjectBasedOnTasksByUser> allProjects = new HashMap<>();

        // Retrieve issues and pull requests by user
        String query = Templates.weeklyReport(selectedLogins, report.week + ".." + DATE_SEARCH_FORMATTER.format(weekEndDate))
                .render();
        JsonObject response = graphQLClient.graphql("Bearer " + token, new JsonObject().put("query", query));
        handleErrors(response);
        JsonObject dataJson = response.getJsonObject("data");

        // Loop among all epics by user
        for (String login : selectedLogins) {
            processIssuesFromSearch(dataJson, login + ISSUES_ASSIGNED, allProjects, login);
            processPullRequestsFromSearch(dataJson, login + PULL_REQUESTS_CREATED, allProjects);
        }

        // Remove everything that does not belong to the select user (if any)
        if (!WeeklyTeamReport.ALL_LOGINS.equals(selectedLogin)) {
            allProjects.values().forEach(project ->
                project.getTasks().keySet().stream()
                        .filter(login -> !selectedLogin.equals(login))
                        .collect(Collectors.toSet())
                        .forEach(login -> project.getTasks().remove(login))
            );
        }

        report.projects.addAll(allProjects.values());

        return report;
    }

    private List<String> getLogins(String selectedLogin) {
        List<String> selectedLogins = new ArrayList<>();
        if (WeeklyTeamReport.ALL_LOGINS.equals(selectedLogin)) {
            selectedLogins.addAll(logins);
        } else {
            selectedLogins.add(selectedLogin);
        }

        return selectedLogins;
    }

    private void processPullRequestsFromSearch(JsonObject dataJson, String type, Map<String, ProjectBasedOnTasksByUser> allProjects) {
        List<Map<String, Task>> allPullRequest = jsonToPullRequestTaskMapper.processPullRequestsFromSearch(dataJson, type);
        for (Map<String, Task> pullRequestByUser : allPullRequest) {
            for (Map.Entry<String, Task> pullRequest : pullRequestByUser.entrySet()) {
                linkTaskToProject(pullRequest.getValue(), allProjects, pullRequest.getKey());
            }
        }
    }

    private void processIssuesFromSearch(JsonObject dataJson, String type, Map<String, ProjectBasedOnTasksByUser> allProjects,
            String defaultAssignee) {
        List<Map<String, Task>> allIssues = jsonToIssueTaskMapper.processIssuesFromSearch(dataJson, type, defaultAssignee);
        for (Map<String, Task> issuesByUser : allIssues) {
            for (Map.Entry<String, Task> issue : issuesByUser.entrySet()) {
                linkTaskToProject(issue.getValue(), allProjects, issue.getKey());
            }

        }
    }

    private void linkTaskToProject(Task task, Map<String, ProjectBasedOnTasksByUser> allProjects, String assignee) {
        // Find project by either label or repository
        ProjectBasedOnTasksByUser project = findProject(allProjects, task.getLabels(), task.getRepoName());

        if (task.getType() == TaskType.PULL_REQUEST && !project.isPullRequestInAnyIssue(task)) {
            project.addTask(assignee, task);
        } else if (task.getType() == TaskType.ISSUE) {
            project.addTask(assignee, task);
        }
    }

    private ProjectBasedOnTasksByUser findProject(Map<String, ProjectBasedOnTasksByUser> allProjects, Set<String> labels, String repoName) {
        String projectName = getLabelValue(PROJECT_LABEL, labels).orElseGet(() -> projectMappingService.mapFromRepoName(repoName));

        ProjectBasedOnTasksByUser project = allProjects.get(projectName);
        if (project == null) {
            project = new ProjectBasedOnTasksByUser(projectName);
            allProjects.put(projectName, project);
        }

        return project;
    }

    private Optional<String> getLabelValue(String key, Set<String> labels) {
        return labels.stream()
                .filter(label -> label.startsWith(key))
                .map(label -> label.replace(key, ""))
                .findFirst();
    }

}