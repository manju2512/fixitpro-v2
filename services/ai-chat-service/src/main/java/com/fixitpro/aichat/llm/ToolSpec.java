package com.fixitpro.aichat.llm;

import java.util.Map;

public record ToolSpec(String type, FunctionDef function) {

    public static ToolSpec function(String name, String description, Map<String, Object> parameters) {
        return new ToolSpec("function", new FunctionDef(name, description, parameters));
    }

    public record FunctionDef(String name, String description, Map<String, Object> parameters) {
    }
}
