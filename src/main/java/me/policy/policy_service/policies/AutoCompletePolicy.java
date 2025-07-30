package me.policy.policy_service.policies;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AutoCompletePolicy {

    private final List<String> filterKeywords;

    public AutoCompletePolicy(@Value("${filter.keywords}") String keywords) {
        this.filterKeywords = List.of(keywords.split(","));
    }

    public List<String> apply(List<String> completions) {
        return completions.stream()
                .filter(completion -> filterKeywords.stream().noneMatch(completion::contains))
                .collect(Collectors.toList());
    }
}
