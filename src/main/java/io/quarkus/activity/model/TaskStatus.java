package io.quarkus.activity.model;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

public enum TaskStatus {
    OPEN("Open"),
    UNKNOWN,
    FINISHED("Closed", "Merged");

    private String[] mappedStatus;

    TaskStatus(String... mappedStatus) {
        this.mappedStatus = mappedStatus;
    }

    @Override
    public String toString() {
        if (mappedStatus == null || mappedStatus.length == 0) {
            return super.toString();
        }

        return mappedStatus[0];
    }

    public static final TaskStatus from(String state) {
        return Stream.of(TaskStatus.values())
                .filter(s -> s.mappedStatus != null && Stream.of(s.mappedStatus)
                        .anyMatch(status -> StringUtils.equalsIgnoreCase(status, state)))
                .findFirst()
                .orElse(TaskStatus.UNKNOWN);
    }
}
