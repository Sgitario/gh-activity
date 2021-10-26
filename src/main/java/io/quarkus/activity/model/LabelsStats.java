package io.quarkus.activity.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class LabelsStats {
    public LocalDateTime updated;
    public Set<String> labels = new HashSet<>();
    public Map<String, String> openIssues = new LinkedHashMap<>();
    public Map<String, String> openPrs = new LinkedHashMap<>();
}
