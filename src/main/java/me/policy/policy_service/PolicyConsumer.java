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
    private final NerService nerService;

    public PolicyConsumer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.nerService = new NerService();  // Initialize NER
    }

    @KafkaListener(topics = "trending-prefixes", groupId = "autocomplete-policy-group")
    public void consume(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            if (root.isArray()) {
                for (JsonNode node : root) {
                    String rawPrefix = node.get("prefix").asText(); // original prefix
                    String nerKey = nerService.extractKey(rawPrefix); // transformed key
                    JsonNode completions = node.get("completions");

                    Document doc = new Document();
                    doc.put("prefix", nerKey);
                    doc.put("completions", completions);

                    mongoTemplate.getCollection("autocomplete_prefixes")
                            .replaceOne(
                                    new Document("prefix", nerKey),
                                    doc,
                                    new com.mongodb.client.model.ReplaceOptions().upsert(true)
                            );

                    System.out.println("Stored generalized prefix: " + nerKey);
                }
            } else {
                System.err.println("Expected a JSON array but got: " + root.toString());
            }

        } catch (IOException e) {
            System.err.println("Failed to parse Kafka message: " + e.getMessage());
        }
    }
}
