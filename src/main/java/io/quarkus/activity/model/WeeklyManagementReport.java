package io.quarkus.activity.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class WeeklyManagementReport {
    public String week;
    public List<ProjectBasedOnTasksAndEpics> projects = new ArrayList<>();

    public long getUpdatedTasksSize() {
        return projects.stream().flatMap(p -> p.getUpdatedTasks().stream()).count();
    }

    public long getRecurrentTasksSize() {
        return projects.stream().flatMap(p -> p.getRecurrentTasks().stream()).count();
    }

    public long getUpdatedEpicsSize() {
        return projects.stream().flatMap(p -> p.getUpdatedEpics().stream()).count();
    }

}