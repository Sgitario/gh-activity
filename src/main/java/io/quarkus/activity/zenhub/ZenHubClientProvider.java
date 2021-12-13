package io.quarkus.activity.zenhub;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.zhapi.ZenHubClient;
import com.zhapi.json.EpicIssueJson;
import com.zhapi.json.responses.GetEpicResponseJson;
import com.zhapi.services.EpicsService;

import io.quarkus.activity.cache.Cacheable;
import io.quarkus.activity.cache.MapCacheable;

@ApplicationScoped
public class ZenHubClientProvider {

    public static final String CACHEABLE_EPICS_ZEN_HUB_CLIENT = "cacheable-epics-zenhub-client";

    @Produces
    public ZenHubClient zenHubClient(@ConfigProperty(name = "activity.zenhub.key") String zenHubApiKey) {
        return new ZenHubClient("https://api.zenhub.io", zenHubApiKey);
    }

    @Produces
    @Named(CACHEABLE_EPICS_ZEN_HUB_CLIENT)
    public MapCacheable<Long, Map<Integer, Cacheable<GetEpicResponseJson>>> epicsZenHubClient(ZenHubClient zenHubClient,
            @ConfigProperty(name = "activity.epics.team.repo-id") List<Long> repoIdsWithEpics) {
        EpicsService epicsService = new EpicsService(zenHubClient);
        MapCacheable<Long, Map<Integer, Cacheable<GetEpicResponseJson>>> epicsZenHubClient = new MapCacheable<>((repoId) -> {
            Map<Integer, Cacheable<GetEpicResponseJson>> epics = new HashMap<>();
            for (EpicIssueJson epicReference : epicsService.getEpics(repoId).getResponse().getEpic_issues()) {
                int issueNumber = epicReference.getIssue_number();
                epics.put(issueNumber, new Cacheable(() -> epicsService.getEpic(repoId, issueNumber).getResponse()));
            }

            return epics;
        });

        // preload the following repos:
        repoIdsWithEpics.forEach(epicsZenHubClient::get);

        return epicsZenHubClient;
    }
}
