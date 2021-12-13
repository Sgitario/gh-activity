package io.quarkus.activity.github;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import io.quarkus.activity.cache.MapCacheable;

@ApplicationScoped
public class GitHubClientProvider {
    public static final String CACHEABLE_REPOSITORY_GITHUB_CLIENT = "cacheable-repos-github-client";

    @Produces
    @Named(CACHEABLE_REPOSITORY_GITHUB_CLIENT)
    public MapCacheable<Long, GHRepository> repositoryGitHubClient(
            @ConfigProperty(name = "activity.token") String token) throws IOException {
        GitHub github = new GitHubBuilder().withOAuthToken(token).build();

        return new MapCacheable<>(repoId -> {
            try {
                return github.getRepositoryById(repoId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
