package io.quarkus.activity.model;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class WeeklyStats {
    public LocalDateTime updated;
    public List<String> weeks = new LinkedList<>();
    public Map<String, List<String>> issuesReported = new LinkedHashMap<>();
    public Map<String, List<String>> issuesAssigned = new LinkedHashMap<>();
    public Map<String, List<String>> issuesParticipant = new LinkedHashMap<>();
    public Map<String, List<String>> prsCreated = new LinkedHashMap<>();
    public Map<String, List<String>> prsReviewed = new LinkedHashMap<>();
}
