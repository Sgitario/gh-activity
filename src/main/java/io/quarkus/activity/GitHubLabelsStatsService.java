package io.quarkus.activity;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.activity.github.GitHubService;
import io.quarkus.activity.model.LabelsStats;
import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class GitHubLabelsStatsService {

    @Inject
    GitHubService gitHubService;

    private volatile LabelsStats stats;

    @Scheduled(every = "6H")
    public void updateLabelsStats() throws IOException {
        stats = buildLabelsStats();
    }

    public LabelsStats getLabelsStats() throws IOException {
        LabelsStats localStats = stats;
        if (localStats == null) {
            synchronized (this) {
                localStats = stats;
                if (stats == null) {
                    stats = localStats = buildLabelsStats();
                }
            }
        }
        return localStats;
    }

    private LabelsStats buildLabelsStats() throws IOException {
        return gitHubService.getLabelsStats();
    }
}
