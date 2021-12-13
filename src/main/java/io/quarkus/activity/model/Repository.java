package io.quarkus.activity.model;

import java.util.regex.Pattern;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Repository {
    public String id;
    public String organization;
    public String name;

    public Repository(String organization, String name) {
        this.organization = organization;
        this.name = name;
        this.id = name.replaceAll("-", "_").replaceAll(Pattern.quote("."), "_");
    }

    @Override
    public String toString() {
        return "Repository{" +
                "organization='" + organization + '\'' +
                ", name='" + name + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
