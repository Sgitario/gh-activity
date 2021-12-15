package io.quarkus.activity.services;

import static io.quarkus.activity.graphql.GraphQLUtils.DATE_SEARCH_FORMATTER;
import static io.quarkus.activity.graphql.GraphQLUtils.handleErrors;
import static io.quarkus.activity.utils.Dates.taskWasCreatedOrUpdatedBetween;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.activity.graphql.GraphQLClient;
import io.quarkus.activity.model.Epic;
import io.quarkus.activity.model.ProjectBasedOnTasksAndEpics;
import io.quarkus.activity.model.Task;
import io.quarkus.activity.model.WeeklyManagementReport;
import io.quarkus.activity.model.mapping.JsonToIssueTaskMapper;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class GitHubWeeklyManagementReportService {

    private static final String UPDATED_TASKS = "updated_tasks";
    private static final String RECURRENT_TASKS = "recurrent_tasks";
    private static final String EPICS = "epics";

    private static final String DEFAULT_ASSIGNEE = "None";
    private static final String PROJECT_LABEL = "project/";

    @ConfigProperty(name = "activity.token")
    String token;

    @Inject
    @RestClient
    GraphQLClient graphQLClient;

    @Inject
    ProjectMappingService projectMappingService;

    @Inject
    DescriptionEpicIssuesLoader descriptionEpicIssuesLoader;

    @Inject
    ZenHubEpicIssuesLoader zenHubEpicIssuesLoader;

    @Inject
    JsonToIssueTaskMapper jsonToIssueTaskMapper;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance weeklyReport(String timeWindow);

        public static native TemplateInstance issues(
                Collection<GitHubWeeklyManagementReportService.RepositoryQuery> repositories);
    }

    public WeeklyManagementReport getWeeklyReport(LocalDate reportStart) throws IOException {
        WeeklyManagementReport report = new WeeklyManagementReport();

        Map<String, ProjectBasedOnTasksAndEpics> allProjects = new HashMap<>();

        LocalDate weekStartDate = reportStart.with(DayOfWeek.MONDAY);
        LocalDate weekEndDate = reportStart.with(DayOfWeek.SUNDAY);
        report.week = DATE_SEARCH_FORMATTER.format(weekStartDate);

        // Retrieve issues and pull requests by user
        String query = Templates.weeklyReport(report.week + ".." + DATE_SEARCH_FORMATTER.format(weekEndDate)).render();
        JsonObject response = graphQLClient.graphql("Bearer " + token, new JsonObject().put("query", query));
        handleErrors(response);
        JsonObject dataJson = response.getJsonObject("data");

        // Process queries
        processUpdatedTasksFromSearch(dataJson, UPDATED_TASKS, allProjects);
        processRecurrentTasksFromSearch(dataJson, RECURRENT_TASKS, allProjects);
        processEpicsFromSearch(dataJson, allProjects, weekStartDate, weekEndDate);

        // Delete projects with no updates
        allProjects.entrySet()
                .stream()
                .filter(entry -> entry.getValue().getUpdatedTasks().isEmpty()
                        && entry.getValue().getRecurrentTasks().isEmpty()
                        && entry.getValue().getUpdatedTasks().isEmpty())
                .map(entry -> entry.getKey())
                .collect(Collectors.toSet())
                .forEach(allProjects::remove);

        report.projects.addAll(allProjects.values());

        return report;
    }

    private void processEpicsFromSearch(JsonObject dataJson, Map<String, ProjectBasedOnTasksAndEpics> allProjects,
            LocalDate weekStartDate, LocalDate weekEndDate) throws IOException {
        List<Map<String, Task>> allIssues = jsonToIssueTaskMapper.processIssuesFromSearch(dataJson, EPICS, DEFAULT_ASSIGNEE);
        for (Map<String, Task> issuesByUser : allIssues) {
            for (Map.Entry<String, Task> issue : issuesByUser.entrySet()) {
                Task epicTask = issue.getValue();

                // Find project by either label or repository
                ProjectBasedOnTasksAndEpics project = findProject(allProjects, epicTask.getLabels(), epicTask.getRepoName());

                // Get linked issues by description
                List<Task> linkedTasks = descriptionEpicIssuesLoader.getLinkedIssues(epicTask, DEFAULT_ASSIGNEE);
                if (linkedTasks.isEmpty()) {
                    // then load linked issues by ZenHub: slow operation
                    linkedTasks = zenHubEpicIssuesLoader.getLinkedIssues(epicTask, DEFAULT_ASSIGNEE);
                }

                // if no linked tasks, but epic was updated OR has linked updated tasks, then add epic
                if ((linkedTasks.isEmpty() && taskWasCreatedOrUpdatedBetween(epicTask, weekStartDate, weekEndDate))
                        || linkedTasks.stream()
                        .anyMatch(task -> taskWasCreatedOrUpdatedBetween(task, weekStartDate, weekEndDate))) {

                    // Find epic
                    Epic epic = getEpicByNameOrCreate(epicTask, project);

                    // enrich epic with progress and other metadata
                    epic.getTasks().addAll(linkedTasks);
                    enrichEpic(epic);
                }
            }
        }
    }

    private void processUpdatedTasksFromSearch(JsonObject dataJson, String type, Map<String, ProjectBasedOnTasksAndEpics> allProjects) {
        List<Map<String, Task>> allIssues = jsonToIssueTaskMapper.processIssuesFromSearch(dataJson, type, DEFAULT_ASSIGNEE);
        for (Map<String, Task> issuesByUser : allIssues) {
            for (Map.Entry<String, Task> issue : issuesByUser.entrySet()) {
                Task task = issue.getValue();

                // Find project by either label or repository
                ProjectBasedOnTasksAndEpics project = findProject(allProjects, task.getLabels(), task.getRepoName());

                project.getUpdatedTasks().add(issue.getValue());
            }
        }
    }

    private void processRecurrentTasksFromSearch(JsonObject dataJson, String type, Map<String, ProjectBasedOnTasksAndEpics> allProjects) {
        List<Map<String, Task>> allIssues = jsonToIssueTaskMapper.processIssuesFromSearch(dataJson, type, DEFAULT_ASSIGNEE);
        for (Map<String, Task> issuesByUser : allIssues) {
            for (Map.Entry<String, Task> issue : issuesByUser.entrySet()) {
                Task task = issue.getValue();

                // Find project by either label or repository
                ProjectBasedOnTasksAndEpics project = findProject(allProjects, task.getLabels(), task.getRepoName());

                project.getRecurrentTasks().add(issue.getValue());
            }
        }
    }

    private void enrichEpic(Epic epic) {
        epic.setTotalTasks(epic.getTasks().size());
        epic.setFinishedTasks((int) epic.getTasks().stream().filter(Task::isClosed).count());
    }

    private Epic getEpicByNameOrCreate(Task task, ProjectBasedOnTasksAndEpics project) {
        Optional<Epic> found = project.getUpdatedEpics().stream().filter(e -> StringUtils.equals(e.getName(), task.getName()))
                .findFirst();
        if (!found.isPresent()) {
            Epic epic = new Epic(task.getName(), task.getUrl(), task);
            project.getUpdatedEpics().add(epic);
            return epic;
        }

        return found.get();
    }

    private ProjectBasedOnTasksAndEpics findProject(Map<String, ProjectBasedOnTasksAndEpics> allProjects, Set<String> labels, String repoName) {
        String projectName = getLabelValue(PROJECT_LABEL, labels).orElseGet(() -> projectMappingService.mapFromRepoName(repoName));

        ProjectBasedOnTasksAndEpics project = allProjects.get(projectName);
        if (project == null) {
            project = new ProjectBasedOnTasksAndEpics(projectName);
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

    public static class RepositoryQuery {
        long repoId;
        String owner;
        String name;
        List<Integer> issues;

        public long getRepoId() {
            return repoId;
        }

        public String getOwner() {
            return owner;
        }

        public String getName() {
            return name;
        }

        public List<Integer> getIssues() {
            return issues;
        }
    }

}