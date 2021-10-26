package io.quarkus.activity;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.activity.github.GitHubService;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

@Path("/")
public class ActivityResource {

    @Inject
    GitHubMonthlyStatsService gitHubMonthlyStatsService;

    @Inject
    GitHubLabelsStatsService gitHubLabelsStatsService;

    @Inject
    GitHubService gitHubService;

    @Inject
    Template labelsStats;

    @Inject
    Template monthlyStats;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getLabelsStats() throws IOException {
        return labelsStats.data("stats", gitHubLabelsStatsService.getLabelsStats());
    }

    @GET
    @Path("/monthly-stats")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getMonthlyStats() throws IOException {
        return monthlyStats.data(
                "logins", gitHubService.getLogins(),
                "stats", gitHubMonthlyStatsService.getMonthlyStats(),
                "colors", defaultColours()
        );
    }

    private Map<String, String> defaultColours() {
        return Map.of("Sgitario", "RED", "geoand", "BLUE");
    }
}
