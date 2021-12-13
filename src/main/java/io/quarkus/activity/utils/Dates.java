package io.quarkus.activity.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import io.quarkus.activity.model.LinkedPullRequest;
import io.quarkus.activity.model.Task;
import io.quarkus.activity.model.TaskType;

public final class Dates {
    private Dates() {

    }

    public static boolean taskWasCreatedOrUpdatedBetween(Task task, LocalDate weekStartDate, LocalDate weekEndDate) {
        Instant start = weekStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = weekEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        return isBetween(task.getCreatedAt(), start, end)
                || isBetween(task.getUpdatedAt(), start, end)
                || (task.getType() == TaskType.ISSUE
                && task.getLinkedPullRequest().stream().anyMatch(pr -> linkedPullRequestWasCreatedOrUpdatedBetween(pr, start, end)));
    }

    private static boolean linkedPullRequestWasCreatedOrUpdatedBetween(LinkedPullRequest linkedPullRequest, Instant start,
            Instant end) {
        return linkedPullRequest != null
                && (linkedPullRequest.getCreatedAt() != null && isBetween(linkedPullRequest.getCreatedAt(), start, end))
                    || (linkedPullRequest.getUpdatedAt() != null && isBetween(linkedPullRequest.getUpdatedAt(), start, end));
    }

    private static boolean isBetween(Instant current, Instant start, Instant end) {
        return current.isAfter(start) && current.isBefore(end);
    }
}
