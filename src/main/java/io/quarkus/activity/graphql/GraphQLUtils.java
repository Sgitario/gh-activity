package io.quarkus.activity.graphql;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public final class GraphQLUtils {

    public static final DateTimeFormatter DATE_SEARCH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private GraphQLUtils() {

    }

    public static void handleErrors(JsonObject response) throws IOException {
        JsonArray errors = response.getJsonArray("errors");
        if (errors != null) {
            // Checking if there are any errors different from NOT_FOUND
            for (int k = 0; k < errors.size(); k++) {
                JsonObject error = errors.getJsonObject(k);
                if (!"NOT_FOUND".equals(error.getString("type"))) {
                    throw new IOException(error.toString());
                }
            }
        }
    }
}
