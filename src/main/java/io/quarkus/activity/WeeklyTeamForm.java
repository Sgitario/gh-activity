package io.quarkus.activity;

import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class WeeklyTeamForm {
    @FormParam("date")
    @PartType(MediaType.TEXT_PLAIN)
    public String date;

    @FormParam("login")
    @PartType(MediaType.TEXT_PLAIN)
    public String login;
}
