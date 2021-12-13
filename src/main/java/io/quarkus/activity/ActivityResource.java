package io.quarkus.activity;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import io.quarkus.activity.model.WeeklyTeamReport;
import io.quarkus.activity.services.GitHubOpenPullRequestsService;
import io.quarkus.activity.services.GitHubWeeklyManagementReportService;
import io.quarkus.activity.services.GitHubWeeklyStatsService;
import io.quarkus.activity.services.GitHubWeeklyTeamReportService;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

@Path("/")
public class ActivityResource {

    @Inject
    GitHubWeeklyStatsService gitHubWeeklyStatsService;

    @Inject
    GitHubWeeklyTeamReportService gitHubWeeklyTeamReportService;

    @Inject
    GitHubWeeklyManagementReportService gitHubWeeklyManagementReportService;

    @Inject
    GitHubOpenPullRequestsService gitHubOpenPullRequestsService;

    @ConfigProperty(name = "activity.logins")
    List<String> logins;

    @Inject
    Template weeklyStats;

    @Inject
    Template weeklyTeamReport;

    @Inject
    Template weeklyManagementReport;

    @Inject
    Template openPrQueue;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getLabelsStats() throws IOException {
        return weeklyStats.data(
                "logins", logins,
                "stats", gitHubWeeklyStatsService.getWeeklyStats());
    }

    @GET
    @Path("/team/weekly-report")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getWeeklyTeamReport() throws IOException {
        return getWeeklyTeamReportByDate(LocalDate.now(), WeeklyTeamReport.ALL_LOGINS);
    }

    @POST
    @Path("/team/weekly-report")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getWeeklyTeamReportByDate(@MultipartForm WeeklyTeamForm weeklyManagementForm) throws IOException {
        return getWeeklyTeamReportByDate(LocalDate.parse(weeklyManagementForm.date), weeklyManagementForm.login);
    }

    @GET
    @Path("/management/weekly-report")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getWeeklyManagementReport() throws IOException {
        return getWeeklyManagementReportByDate(LocalDate.now());
    }

    @POST
    @Path("/management/weekly-report")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getWeeklyManagementReportByDate(@MultipartForm WeeklyManagementForm weeklyManagementForm)
            throws IOException {
        return getWeeklyManagementReportByDate(LocalDate.parse(weeklyManagementForm.date));
    }

    @GET
    @Path("/open-pr-queue")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getOpenPrsQueue() throws IOException {
        return openPrQueue.data(
                "logins", logins,
                "result", gitHubOpenPullRequestsService.getOpenPrQueueInOrganization()
        );
    }

    private TemplateInstance getWeeklyTeamReportByDate(LocalDate date, String login) throws IOException {
        return weeklyTeamReport.data(
                "logins", logins,
                "report", gitHubWeeklyTeamReportService.getWeeklyReport(date, login));
    }

    private TemplateInstance getWeeklyManagementReportByDate(LocalDate date) throws IOException {
        return weeklyManagementReport.data(
                "logins", logins,
                "report", gitHubWeeklyManagementReportService.getWeeklyReport(date));
    }
}
