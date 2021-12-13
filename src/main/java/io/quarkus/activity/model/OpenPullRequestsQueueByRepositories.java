package io.quarkus.activity.model;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class OpenPullRequestsQueueByRepositories {
    public LocalDateTime updated;
    public Map<String, List<OpenPullRequest>> repositories = new LinkedHashMap<>();
}