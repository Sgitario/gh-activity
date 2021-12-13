package io.quarkus.activity.model.mapping;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import io.vertx.core.json.JsonObject;

public abstract class BaseTaskMapper {

    private static final String REPOSITORY_TOKEN = "Repository";

    protected long extractRepoId(String encodedRepoId) {
        String decodedRepoId = new String(Base64.getDecoder().decode(encodedRepoId));
        int decodedRepositoryPos = decodedRepoId.lastIndexOf(REPOSITORY_TOKEN);
        return Long.parseLong(decodedRepoId.substring(decodedRepositoryPos + REPOSITORY_TOKEN.length()));
    }

    protected Set<String> getLabels(JsonObject objectJson) {
        Set<String> labels = new HashSet<>();
        if (objectJson.containsKey("labels")) {
            objectJson.getJsonObject("labels").getJsonArray("nodes")
                    .stream()
                    .map(json -> ((JsonObject) json).getString("name"))
                    .forEach(labels::add);
        }

        return labels;
    }
}
