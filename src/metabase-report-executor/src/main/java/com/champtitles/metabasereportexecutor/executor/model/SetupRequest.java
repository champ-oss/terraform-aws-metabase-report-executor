package com.champtitles.metabasereportexecutor.executor.model;

public record SetupRequest(String token, SetupPrefsRequest prefs, String database, SetupUserRequest user) {

    public SetupRequest(String setupToken, String email, String password) {
        this(setupToken, new SetupPrefsRequest("test", "en", false),
                null, new SetupUserRequest("test", "test", email, password, "test"));
    }
}
