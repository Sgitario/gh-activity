package io.quarkus.activity.services;

import static io.quarkus.activity.github.GitHubClientProvider.CACHEABLE_REPOSITORY_GITHUB_CLIENT;
import static io.quarkus.activity.graphql.GraphQLUtils.DATE_SEARCH_FORMATTER;
import static io.quarkus.activity.graphql.GraphQLUtils.handleErrors;
import static io.quarkus.activity.utils.Dates.taskWasCreatedOrUpdatedBetween;
import static io.quarkus.activity.zenhub.ZenHubClientProvider.CACHEABLE_EPICS_ZEN_HUB_CLIENT;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.kohsuke.github.GHRepository;

import com.zhapi.json.GetEpicIssueEntryJson;
import com.zhapi.json.responses.GetEpicResponseJson;

import io.quarkus.activity.cache.Cacheable;
import io.quarkus.activity.cache.MapCacheable;
import io.quarkus.activity.graphql.GraphQLClient;
import io.quarkus.activity.model.Epic;
import io.quarkus.activity.model.ProjectBasedOnTasksAndEpics;
import io.quarkus.activity.model.Task;
import io.quarkus.activity.model.WeeklyManagementReport;
import io.quarkus.activity.model.mapping.JsonToIssueTaskMapper;
import io.quarkus.activity.model.mapping.JsonToPullRequestTaskMapper;
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
    @Named(CACHEABLE_EPICS_ZEN_HUB_CLIENT)
    MapCacheable<Long, Map<Integer, Cacheable<GetEpicResponseJson>>> epicsZenHubClient;

    @Inject
    @Named(CACHEABLE_REPOSITORY_GITHUB_CLIENT)
    MapCacheable<Long, GHRepository> repositoriesGitHubClient;

    @Inject
    ProjectMappingService projectMappingService;

    @Inject
    JsonToPullRequestTaskMapper jsonToPullRequestTaskMapper;

    @Inject
    JsonToIssueTaskMapper jsonToIssueTaskMapper;

    @CheckedTemplate
    private static class Templates {
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

                List<Task> linkedTasks = Collections.emptyList();

                // If it's closed, we don't enrich the epic with issues
                if (!epicTask.isClosed()) {
                    linkedTasks = enrichZenHubEpicLinkedIssues(epicTask, DEFAULT_ASSIGNEE);
                }

                // if no linked tasks, but epic was updated OR has linked updated tasks, then add epic
                if ((linkedTasks.isEmpty() && taskWasCreatedOrUpdatedBetween(epicTask, weekStartDate, weekEndDate))
                        || linkedTasks.stream().anyMatch(task -> taskWasCreatedOrUpdatedBetween(task, weekStartDate, weekEndDate))) {

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

    private List<Task> enrichZenHubEpicLinkedIssues(Task epicTask, String defaultAssignee) throws IOException {
        Map<Integer, Cacheable<GetEpicResponseJson>> epicResponse = epicsZenHubClient.get(epicTask.getRepoId());
        if (epicResponse == null) {
            // ZenHub does not have information about this epic, we ignore it.
            return Collections.emptyList();
        }

        Cacheable<GetEpicResponseJson> cachedEpicFromZenHub = epicResponse.get(epicTask.getId());
        if (cachedEpicFromZenHub == null) {
            // ZenHub could not deal with the epic ID.
            return Collections.emptyList();
        }

        GetEpicResponseJson epicFromZenHub = cachedEpicFromZenHub.get();
        if (epicFromZenHub == null) {
            // ZenHub returned null
            return Collections.emptyList();
        }

        List<Task> linkedTasks = new ArrayList<>();
        Map<Long, RepositoryQuery> issuesByRepository = new HashMap<>();
        for (GetEpicIssueEntryJson issue : epicFromZenHub.getIssues()) {
            RepositoryQuery repositoryQuery = issuesByRepository.get(issue.getRepo_id());
            if (repositoryQuery == null) {
                GHRepository repository = repositoriesGitHubClient.get(issue.getRepo_id());

                repositoryQuery = new RepositoryQuery();
                repositoryQuery.repoId = issue.getRepo_id();
                repositoryQuery.owner = repository.getOwnerName();
                repositoryQuery.name = repository.getName();
                repositoryQuery.issues = new ArrayList<>();
                issuesByRepository.put(issue.getRepo_id(), repositoryQuery);
            }

            repositoryQuery.issues.add(issue.getIssue_number());
        }

        // Retrieve issues and pull requests by user
        String query = Templates.issues(issuesByRepository.values()).render();
        JsonObject response = graphQLClient.graphql("Bearer " + token, new JsonObject().put("query", query));
        handleErrors(response);
        JsonObject dataJson = response.getJsonObject("data");

        for (RepositoryQuery repository : issuesByRepository.values()) {
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