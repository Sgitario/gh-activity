package io.quarkus.activity.model;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class WeeklyTeamReport {

    public static final String ALL_LOGINS = "ALL";

    public String week;
    public String selectedLogin = ALL_LOGINS;
    public List<ProjectBasedOnTasksByUser> projects;
}