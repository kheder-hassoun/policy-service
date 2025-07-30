package me.policy.policy_service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class PrefixSuggestion {
    private String prefix;

    @JsonProperty("completions")
    private List<String> completions;

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public List<String> getCompletions() { return completions; }
    public void setCompletions(List<String> completions) { this.completions = completions; }
}