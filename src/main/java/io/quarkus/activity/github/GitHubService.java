package io.quarkus.activity.github;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GitHub;

import io.quarkus.activity.graphql.GraphQLClient;
import io.quarkus.activity.model.LabelsStats;
import io.quarkus.activity.model.MonthlyStats;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class GitHubService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Inject
    @RestClient
    GraphQLClient graphQLClient;

    @ConfigProperty(name = "activity.logins", defaultValue = "rsvoboda,mjurc,pjgg,Sgitario,kshpak,jsmrcka,fedinskiy")
    List<String> logins;

    @ConfigProperty(name = "activity.labels.includes", defaultValue = "area")
    List<String> labelsToInclude;

    @ConfigProperty(name = "activity.token")
    String token;

    public List<String> getLogins() {
        return logins;
    }

    public MonthlyStats getMonthlyStats(LocalDate statsStart) throws IOException {
        MonthlyStats stats = new MonthlyStats();
        stats.updated = LocalDateTime.now();
        for (String login : logins) {
            stats.quarkusioIssuesReported.put(login, new LinkedList<>());
            stats.quarkusioIssuesAssigned.put(login, new LinkedList<>());
            stats.quarkusioIssuesParticipant.put(login, new LinkedList<>());
            stats.quarkusioPRsCreated.put(login, new LinkedList<>());
            stats.quarkusioPRsReviewed.put(login, new LinkedList<>());
        }

        LocalDate start = statsStart;
        LocalDate stopTime = LocalDate.now().withDayOfMonth(2);

        while (start.isBefore(stopTime)) {
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            String timeWindow = start + ".." + end;
            stats.months.add(FORMATTER.format(start));

            String query = Templates.monthlyActivity(logins, timeWindow).render();
            JsonObject response = graphQLClient.graphql("Bearer " + token, new JsonObject().put("query", query));
            handleErrors(response);

            JsonObject dataJson = response.getJsonObject("data");

            for (String login : logins) {

                stats.quarkusioIssuesReported.get(login)
                        .add(dataJson.getJsonObject(login + "_issues_reported_quarkusio").getString("issueCount"));
                stats.quarkusioIssuesAssigned.get(login)
                        .add(dataJson.getJsonObject(login + "_issues_assigned_quarkusio").getString("issueCount"));
                stats.quarkusioIssuesParticipant.get(login)
                        .add(dataJson.getJsonObject(login + "_issues_participant_quarkusio").getString("issueCount"));
                stats.quarkusioPRsCreated.get(login)
                        .add(dataJson.getJsonObject(login + "_prs_created_quarkusio").getString("issueCount"));
                stats.quarkusioPRsReviewed.get(login)
                        .add(dataJson.getJsonObject(login + "_prs_reviewed_quarkusio").getString("issueCount"));
            }

            start = start.plusMonths(1);
        }
        return stats;
    }

    public LabelsStats getLabelsStats() throws IOException {
        LabelsStats stats = new LabelsStats();
        stats.updated = LocalDateTime.now();
        stats.labels = getLabels();
        Map<String, String> formattedLabels = stats.labels.stream()
                .collect(Collectors.toMap(
                        label -> label.replaceAll("/", "_").replaceAll("-", "_").replaceAll(" ", "_"),
                        label -> label));
        String query = Templates.labelsStats(formattedLabels).render();
        JsonObject response = graphQLClient.graphql("Bearer " + token, new JsonObject().put("query", query));
        handleErrors(response);

        JsonObject dataJson = response.getJsonObject("data");
        for (Map.Entry<String, String> label : formattedLabels.entrySet()) {
            stats.openIssues.put(label.getValue(),
                    dataJson.getJsonObject(label.getKey() + "_issues_opened").getString("issueCount"));
            stats.openPrs.put(label.getValue(),
                    dataJson.getJsonObject(label.getKey() + "_prs_opened").getString("issueCount"));
        }

        return stats;
    }

    private Set<String> getLabels() throws IOException {
        Set<String> labels = new HashSet<>();
        GitHub github = GitHub.connectUsingOAuth(token);
        for (GHLabel label : github.getRepository("quarkusio/quarkus").listLabels()) {
            // Add it if it's included
            if (labelsToInclude.stream().anyMatch(inclusion -> label.getName().contains(inclusion))) {
                labels.add(label.getName());
            }
        }

        return labels;
    }

    @CheckedTemplate
    private static class Templates {
        public static native TemplateInstance labelsStats(Map<String, String> labels);

        public static native TemplateInstance monthlyActivity(Collection<String> logins, String timeWindow);
    }

    private void handleErrors(JsonObject response) throws IOException {
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