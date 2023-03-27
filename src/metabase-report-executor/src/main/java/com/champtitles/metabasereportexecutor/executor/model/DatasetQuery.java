package com.champtitles.metabasereportexecutor.executor.model;

import java.util.HashMap;
import java.util.Map;

public record DatasetQuery(String type, Integer database, Map<String, Object> query) {

    public DatasetQuery(String type) {
        this(type, 1, new HashMap<>() {{
            put("source-table", 1);
        }});
    }
}
