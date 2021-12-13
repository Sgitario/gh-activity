package io.quarkus.activity.services;

import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.activity.project.ProjectMappingConfiguration;

@ApplicationScoped
public class ProjectMappingService {

    @Inject
    ProjectMappingConfiguration config;

    public String mapFromRepoName(String repoName) {
        if (repoName != null) {
            for (Map.Entry<String, String> rule : config.map().entrySet()) {
                if (repoName.matches(rule.getValue())) {
                    return rule.getKey();
                }
            }
        }

        return Optional.ofNullable(repoName).orElse("Unknown");
    }
}
