package me.policy.policy_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PolicyConsumer {

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PolicyConsumer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @KafkaListener(topics = "trending-prefixes", groupId = "autocomplete-policy-group")
    public void consume(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            if (root.isArray()) {
                for (JsonNode node : root) {
                    String prefix = node.get("prefix").asText();
                    JsonNode completions = node.get("completions");

                    Document doc = new Document();
                    doc.put("prefix", prefix);
                    doc.put("completions", completions);

                    mongoTemplate.getCollection("autocomplete_prefixes")
                            .replaceOne(
                                    new Document("prefix", prefix),
                                    doc,
                                    new com.mongodb.client.model.ReplaceOptions().upsert(true)
                            );

                    System.out.println(" Stored prefix: " + prefix);
                }
            } else {
                System.err.println(" Expected a JSON array but got: " + root.toString());
            }

        } catch (IOException e) {
            System.err.println(" Failed to parse Kafka message: " + e.getMessage());
        }
    }
}
