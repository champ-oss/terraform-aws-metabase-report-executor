package com.champtitles.metabasereportexecutor.executor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SetupUserRequest(@JsonProperty("first_name") String firstName,
                               @JsonProperty("last_name") String lastName,
                               String email,
                               String password,
                               @JsonProperty("site_name") String siteName) {
}
