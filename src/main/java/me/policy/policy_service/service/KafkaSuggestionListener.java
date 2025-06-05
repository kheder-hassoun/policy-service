package me.policy.policy_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaSuggestionListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public KafkaSuggestionListener(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = "autocomplete.autocomplete.prefix_suggestions", groupId = "policy-group")
    public void listen(String message) {
        try {
            JsonNode root = mapper.readTree(message);
            JsonNode after = root.get("payload").get("after");

            if (after != null && !after.isNull()) {
                String prefix = after.get("prefix").asText();
                String completionsJson = after.get("completions").asText();

                // Convert completions JSON string to list
                JsonNode completionsNode = mapper.readTree(completionsJson);
                redisTemplate.opsForValue().set(prefix, completionsNode.toString());
                System.out.println("Stored in Redis: " + prefix + " -> " + completionsNode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}