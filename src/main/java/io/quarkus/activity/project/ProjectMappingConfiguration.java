package io.quarkus.activity.project;

import java.util.Map;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "activity.project")
public interface ProjectMappingConfiguration {
    Map<String, String> map();
}
