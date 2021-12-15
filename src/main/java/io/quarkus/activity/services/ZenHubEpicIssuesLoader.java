package io.quarkus.activity.services;

import static io.quarkus.activity.github.GitHubClientProvider.CACHEABLE_REPOSITORY_GITHUB_CLIENT;
import static io.quarkus.activity.zenhub.ZenHubClientProvider.CACHEABLE_EPICS_ZEN_HUB_CLIENT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.kohsuke.github.GHRepository;

import com.zhapi.json.GetEpicIssueEntryJson;
import com.zhapi.json.responses.GetEpicResponseJson;

import io.quarkus.activity.cache.Cacheable;
import io.quarkus.activity.cache.MapCacheable;
import io.quarkus.activity.model.Task;

@ApplicationScoped
public class ZenHubEpicIssuesLoader extends BaseEpicIssuesLoader {

    @Inject
    @Named(CACHEABLE_EPICS_ZEN_HUB_CLIENT)
    MapCacheable<Long, Map<Integer, Cacheable<GetEpicResponseJson>>> epicsZenHubClient;

    @Inject
    @Named(CACHEABLE_REPOSITORY_GITHUB_CLIENT)
    MapCacheable<Long, GHRepository> repositoriesGitHubClient;

    public List<Task> getLinkedIssues(Task epicTask, String defaultAssignee) throws IOException {
        // If it's closed, we don't enrich the epic with issues
        if (!epicTask.isClosed()) {
            return enrichZenHubEpicLinkedIssues(epicTask, defaultAssignee);
        }

        return Collections.emptyList();
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

        Map<Long, GitHubWeeklyManagementReportService.RepositoryQuery> issuesByRepository = new HashMap<>();
        for (GetEpicIssueEntryJson issue : epicFromZenHub.getIssues()) {
            GitHubWeeklyManagementReportService.RepositoryQuery repositoryQuery = issuesByRepository.get(issue.getRepo_id());
            if (repositoryQuery == null) {
                GHRepository repository = repositoriesGitHubClient.get(issue.getRepo_id());

                repositoryQuery = new GitHubWeeklyManagementReportService.RepositoryQuery();
                repositoryQuery.repoId = issue.getRepo_id();
                repositoryQuery.owner = repository.getOwnerName();
                repositoryQuery.name = repository.getName();
                repositoryQuery.issues = new ArrayList<>();
                issuesByRepository.put(issue.getRepo_id(), repositoryQuery);
            }

            repositoryQuery.issues.add(issue.getIssue_number());
        }

        return getTasksUsingGitHub(defaultAssignee, issuesByRepository);
    }
}
