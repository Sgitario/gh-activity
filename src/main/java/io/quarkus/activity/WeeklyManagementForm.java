package io.quarkus.activity;

import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class WeeklyManagementForm {
    @FormParam("date")
    @PartType(MediaType.TEXT_PLAIN)
    public String date;
}
