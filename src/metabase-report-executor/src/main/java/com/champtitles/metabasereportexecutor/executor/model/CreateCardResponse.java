package com.champtitles.metabasereportexecutor.executor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateCardResponse(String id) {
}
