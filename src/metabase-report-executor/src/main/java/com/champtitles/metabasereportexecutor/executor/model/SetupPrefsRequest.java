package com.champtitles.metabasereportexecutor.executor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SetupPrefsRequest(@JsonProperty("site_name") String siteName,
                                @JsonProperty("site_locale") String siteLocale,
                                @JsonProperty("allow_tracking") Boolean allowTracking) {
}
