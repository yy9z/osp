package com.caspar.agent.model;

import lombok.Data;

@Data
public class LlmMessage {
    private String role;
    private String content;

    public static LlmMessage system(String content) {
        LlmMessage m = new LlmMessage();
        m.setRole("system");
        m.setContent(content);
        return m;
    }

    public static LlmMessage user(String content) {
        LlmMessage m = new LlmMessage();
        m.setRole("user");
        m.setContent(content);
        return m;
    }

    public static LlmMessage assistant(String content) {
        LlmMessage m = new LlmMessage();
        m.setRole("assistant");
        m.setContent(content);
        return m;
    }
}
