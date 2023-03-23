package com.champtitles.metabasereportexecutor.executor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public record CreateCardRequest(
        String name,
        String display,
        @JsonProperty("dataset_query") DatasetQuery datasetQuery,
        @JsonProperty("visualization_settings") Map<String, Object> visualizationSettings) {

    public CreateCardRequest(String name) {
        this(name, "bar", new DatasetQuery("query"), new HashMap<>());
    }
}