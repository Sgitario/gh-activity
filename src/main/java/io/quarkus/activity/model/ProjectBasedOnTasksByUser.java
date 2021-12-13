package io.quarkus.activity.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ProjectBasedOnTasksByUser {
    private final String name;
    private final Map<String, Set<Task>> tasks = new HashMap<>();

    public ProjectBasedOnTasksByUser(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Map<String, Set<Task>> getTasks() {
        return tasks;
    }

    public void addTask(String login, Task task) {
        Set<Task> tasksByUser = tasks.get(login);
        if (tasksByUser == null) {
            tasksByUser = new HashSet<>();
            tasks.put(login, tasksByUser);
        }

        tasksByUser.add(task);
    }

    public boolean isPullRequestInAnyIssue(Task pullRequest) {
        // It's part of an unlinked issue:
        return tasks.values().stream()
                .flatMap(Collection::stream)
                .anyMatch(taskInEpic -> taskInEpic.isLinkedPullRequestFound(pullRequest.getUrl()));
    }
}
