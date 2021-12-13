package io.quarkus.activity.model;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ProjectBasedOnTasksAndEpics {

    private final String name;
    private final List<Task> updatedTasks = new ArrayList<>();
    private final List<Task> recurrentTasks = new ArrayList<>();
    private final List<Epic> updatedEpics = new ArrayList<>();

    public ProjectBasedOnTasksAndEpics(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Epic> getUpdatedEpics() {
        return updatedEpics;
    }

    public List<Task> getUpdatedTasks() {
        return updatedTasks;
    }

    public List<Task> getRecurrentTasks() {
        return recurrentTasks;
    }
}
