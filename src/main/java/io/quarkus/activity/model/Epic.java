package io.quarkus.activity.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Epic {

    private final String name;
    private final String url;
    private final List<Task> tasks = new ArrayList<>();
    private final Task data;

    private int finishedTasks;
    private int totalTasks;

    public Epic(String name, String url, Task data) {
        this.name = name;
        this.url = url;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public Task getData() {
        return data;
    }

    public int getFinishedTasks() {
        return finishedTasks;
    }

    public void setFinishedTasks(int finishedTasks) {
        this.finishedTasks = finishedTasks;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Epic epic = (Epic) o;
        return name.equals(epic.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
