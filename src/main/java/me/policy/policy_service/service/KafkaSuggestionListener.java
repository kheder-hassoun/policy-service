package me.policy.policy_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.policy.policy_service.policies.AutoCompletePolicy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KafkaSuggestionListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AutoCompletePolicy policy;
    private final ObjectMapper mapper = new ObjectMapper();

    public KafkaSuggestionListener(RedisTemplate<String, Object> redisTemplate, AutoCompletePolicy policy) {
        this.redisTemplate = redisTemplate;
        this.policy = policy;
    }

    @KafkaListener(topics = "autocomplete.autocomplete.prefix_suggestions", groupId = "policy-group")
    public void listen(String message) {
        try {
            JsonNode root = mapper.readTree(message);
            JsonNode after = root.get("payload").get("after");

            if (after != null && !after.isNull()) {
                String prefix = after.get("prefix").asText();
                String completionsJson = after.get("completions").asText();

                List<String> completions = mapper.readValue(
                        completionsJson,
                        mapper.getTypeFactory().constructCollectionType(List.class, String.class)
                );

                List<String> filtered = policy.apply(completions);

                redisTemplate.opsForValue().set(prefix, mapper.writeValueAsString(filtered));
                System.out.println("Stored in Redis: " + prefix + " -> " + filtered);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
