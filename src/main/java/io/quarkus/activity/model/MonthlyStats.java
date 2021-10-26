package io.quarkus.activity.model;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class MonthlyStats {
    public LocalDateTime updated;
    public List<String> months = new LinkedList<>();
    public Map<String, List<String>> quarkusioIssuesReported = new LinkedHashMap<>();
    public Map<String, List<String>> quarkusioIssuesAssigned = new LinkedHashMap<>();
    public Map<String, List<String>> quarkusioIssuesParticipant = new LinkedHashMap<>();
    public Map<String, List<String>> quarkusioPRsCreated = new LinkedHashMap<>();
    public Map<String, List<String>> quarkusioPRsReviewed = new LinkedHashMap<>();
}
