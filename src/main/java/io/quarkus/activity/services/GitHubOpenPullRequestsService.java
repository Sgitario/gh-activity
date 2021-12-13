package io.quarkus.activity.services;

import static io.quarkus.activity.graphql.GraphQLUtils.handleErrors;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.activity.graphql.GraphQLClient;
import io.quarkus.activity.model.OpenPullRequest;
import io.quarkus.activity.model.OpenPullRequestsQueueByRepositories;
import io.quarkus.activity.model.Repository;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.scheduler.Scheduled;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GitHubOpenPullRequestsService {
    private volatile OpenPullRequestsQueueByRepositories openPrQueueInOrganization;

    @ConfigProperty(name = "activity.pull-requests.organizations")
    List<String> organizations;

    @ConfigProperty(name = "activity.pull-requests.repositories")
    List<String> repositories;

    @ConfigProperty(name = "activity.token")
    String token;

    @ConfigProperty(name = "activity.limit", defaultValue = "100")
    int limit;

    @Inject
    @RestClient
    GraphQLClient graphQLClient;

    @Scheduled(every = "10m")
    public void updateOpenPrQueueInOrganization() throws IOException {
        openPrQueueInOrganization = buildOpenPrQueueInOrganization();
    }

    public OpenPullRequestsQueueByRepositories getOpenPrQueueInOrganization() throws IOException {
        OpenPullRequestsQueueByRepositories localStats = openPrQueueInOrganization;
        if (localStats == null) {
            synchronized (this) {
                localStats = openPrQueueInOrganization;
                if (openPrQueueInOrganization == null) {
                    openPrQueueInOrganization = localStats = buildOpenPrQueueInOrganization();
                }
            }
        }

        return localStats;
    }

    @CheckedTemplate
    private static class Templates {
        public static native TemplateInstance openPullRequestsInRepositories(Collection<Repository> repositories,
                String organization, Integer limit);

        public static native TemplateInstance repositoriesByOrganization(String organization, Integer limit);
    }

    private OpenPullRequestsQueueByRepositories buildOpenPrQueueInOrganization() throws IOException {
        OpenPullRequestsQueueByRepositories result = new OpenPullRequestsQueueByRepositories();

        Map<String, Map<String, Repository>> repositoriesToWatch = getRepositoriesToWatch();

        for (Map.Entry<String, Map<String, Repository>> repositoriesByOrganization : repositoriesToWatch.entrySet()) {
            String organizationName = repositoriesByOrganization.getKey();
            Collection<Repository> repositoriesInOrganization = repositoriesByOrganization.getValue().values();

            String query = Templates.openPullRequestsInRepositories(repositoriesInOrganization, organizationName, limit)
                    .render();
            JsonObject response = graphQLClient.graphql("Bearer " + token, new JsonObject().put("query", query));
            handleErrors(response);

            JsonObject dataJson = response.getJsonObject("data");

            for (Repository repository : repositoriesInOrganization) {
                JsonArray pullRequestsNodesJson = dataJson.getJsonObject(repository.id + "_open_prs")
                        .getJsonObject("pullRequests")
                        .getJsonArray("nodes");

                List<OpenPullRequest> pullRequests = pullRequestsNodesJson.stream().map(prItem -> {
                    JsonObject prJsonItem = (JsonObject) prItem;
                    Instant createdAt = prJsonItem.getInstant("createdAt");
                    String title = prJsonItem.getString("title");
                    String repositoryUrl = prJsonItem.getString("url");
                    String author = prJsonItem.getJsonObject("author").getString("login");
                    return new OpenPullRequest(createdAt, title, repositoryUrl, author);
                }).collect(Collectors.toList());

                // Filter out repositories without pull requests
                if (!pullRequests.isEmpty()) {
                    result.repositories.put(repository.organization + "/" + repository.name, pullRequests);
                }
            }
        }

        result.updated = LocalDateTime.now();

        return result;
    }

    private Map<String, Map<String, Repository>> getRepositoriesToWatch() throws IOException {
        Map<String, Map<String, Repository>> repositoriesToWatch = new HashMap<>();
        // Using organization property
        for (String organizationName : organizations) {
            String query = Templates.repositoriesByOrganization(organizationName, limit).render();
            JsonObject response = graphQLClient.graphql("Bearer " + token, new JsonObject().put("query", query));
            handleErrors(response);

            Map<String, Repository> reposInOrganization = getOrCreateRepositoriesInOrganization(repositoriesToWatch,
                    organizationName);

            JsonArray dataJson = response.getJsonObject("data").getJsonObject("search").getJsonArray("edges");
            dataJson.stream()
                    .map(item -> ((JsonObject) item).getJsonObject("node").getString("name"))
                    .forEach(repositoryName -> reposInOrganization.put(repositoryName,
                            new Repository(organizationName, repositoryName)));
        }

        // Using repositories property
        for (String repository : repositories) {
            String[] repositoryNameSplit = repository.split("/");
            String organizationName = repositoryNameSplit[0];
            String repositoryName = repositoryNameSplit[1];

            getOrCreateRepositoriesInOrganization(repositoriesToWatch, organizationName).put(repositoryName,
                    new Repository(organizationName, repositoryName));
        }
        return repositoriesToWatch;
    }

    private Map<String, Repository> getOrCreateRepositoriesInOrganization(
            Map<String, Map<String, Repository>> repositoriesToWatch,
            String organizationName) {
        Map<String, Repository> reposInOrganization = repositoriesToWatch.get(organizationName);
        if (reposInOrganization == null) {
            reposInOrganization = new HashMap<>();
            repositoriesToWatch.put(organizationName, reposInOrganization);
        }

        return reposInOrganization;
    }
}
