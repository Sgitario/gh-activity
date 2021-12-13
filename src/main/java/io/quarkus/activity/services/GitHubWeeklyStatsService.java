package io.quarkus.activity.services;

import static io.quarkus.activity.graphql.GraphQLUtils.DATE_SEARCH_FORMATTER;
import static io.quarkus.activity.graphql.GraphQLUtils.handleErrors;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.activity.graphql.GraphQLClient;
import io.quarkus.activity.model.WeeklyStats;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.scheduler.Scheduled;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class GitHubWeeklyStatsService {

    private static final String ISSUES_ASSIGNED = "_issues_assigned";
    private static final String ISSUES_REPORTED = "_issues_reported";
    private static final String ISSUES_COMMENTED = "_issues_participant";
    private static final String PULL_REQUESTS_CREATED = "_prs_created";
    private static final String PULL_REQUESTS_REVIEWED = "_prs_reviewed";

    @ConfigProperty(name = "activity.start")
    LocalDate statsStart;

    @ConfigProperty(name = "activity.logins")
    List<String> logins;

    @ConfigProperty(name = "activity.token")
    String token;

    @Inject
    @RestClient
    GraphQLClient graphQLClient;

    private volatile WeeklyStats weeklyStats;

    @Scheduled(every = "6H")
    public void updateWeeklyStats() throws IOException {
        weeklyStats = buildWeeklyStats();
    }

    public WeeklyStats getWeeklyStats() throws IOException {
        WeeklyStats localStats = weeklyStats;
        if (localStats == null) {
            synchronized (this) {
                localStats = weeklyStats;
                if (weeklyStats == null) {
                    weeklyStats = localStats = buildWeeklyStats();
                }
            }
        }
        return localStats;
    }

    @CheckedTemplate
    private static class Templates {
        public static native TemplateInstance weeklyActivity(Collection<String> logins, String timeWindow);
    }

    private WeeklyStats buildWeeklyStats() throws IOException {
        WeeklyStats stats = new WeeklyStats();
        stats.updated = LocalDateTime.now();
        for (String login : logins) {
            stats.issuesReported.put(login, new LinkedList<>());
            stats.issuesAssigned.put(login, new LinkedList<>());
            stats.issuesParticipant.put(login, new LinkedList<>());
            stats.prsCreated.put(login, new LinkedList<>());
            stats.prsReviewed.put(login, new LinkedList<>());
        }

        LocalDate start = statsStart;
        LocalDate stopTime = LocalDate.now();

        while (start.isBefore(stopTime)) {
            String startDateWithFormat = DATE_SEARCH_FORMATTER.format(start);
            LocalDate end = start.plusWeeks(1);
            String timeWindow = startDateWithFormat + ".." + DATE_SEARCH_FORMATTER.format(end);
            stats.weeks.add(startDateWithFormat);

            String query = Templates.weeklyActivity(logins, timeWindow).render();
            JsonObject response = graphQLClient.graphql("Bearer " + token, new JsonObject().put("query", query));
            handleErrors(response);

            JsonObject dataJson = response.getJsonObject("data");

            for (String login : logins) {

                stats.issuesReported.get(login)
                        .add(dataJson.getJsonObject(login + ISSUES_REPORTED).getString("issueCount"));
                stats.issuesAssigned.get(login)
                        .add(dataJson.getJsonObject(login + ISSUES_ASSIGNED).getString("issueCount"));
                stats.issuesParticipant.get(login)
                        .add(dataJson.getJsonObject(login + ISSUES_COMMENTED).getString("issueCount"));
                stats.prsCreated.get(login)
                        .add(dataJson.getJsonObject(login + PULL_REQUESTS_CREATED).getString("issueCount"));
                stats.prsReviewed.get(login)
                        .add(dataJson.getJsonObject(login + PULL_REQUESTS_REVIEWED).getString("issueCount"));
            }

            start = start.plusWeeks(1);
        }

        return stats;
    }
}
