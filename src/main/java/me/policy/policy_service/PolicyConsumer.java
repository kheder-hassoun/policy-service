package me.policy.policy_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class PolicyConsumer {

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NerService nerService;

    public PolicyConsumer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.nerService = new NerService();
    }

    @KafkaListener(topics = "trending-prefixes", groupId = "autocomplete-policy-group")
    public void consume(String message) {
        System.out.println(" Received raw Kafka message: " + message);

        try {
            JsonNode root = objectMapper.readTree(message);

            if (root.isObject()) {
                // 1. Extract prefix and completions
                String rawPrefix = root.get("prefix").asText();
                JsonNode completionsNode = root.get("completions");

                // 2. Apply NER generalization
                String generalizedKey = nerService.extractKey(rawPrefix);

                // 3. Convert completionsNode to a List of Maps
                List<Map<String, Object>> completionsList = new ArrayList<>();
                for (JsonNode completion : completionsNode) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("query", completion.get("query").asText());
                    entry.put("frequency", completion.get("frequency").asInt());
                    entry.put("last_updated", completion.get("last_updated").asText());
                    completionsList.add(entry);
                }

                // 4. Construct MongoDB document
                Document doc = new Document();
                doc.put("prefix", generalizedKey);
                doc.put("completions", completionsList);

                // 5. Upsert into MongoDB
                mongoTemplate.getCollection("autocomplete_prefixes")
                        .replaceOne(
                                new Document("prefix", generalizedKey),
                                doc,
                                new com.mongodb.client.model.ReplaceOptions().upsert(true)
                        );

                System.out.println("  Stored: " + generalizedKey + " → " + completionsList.size() + " completions");
            } else {
                System.err.println("  Expected JSON object but got: " + root.toString());
            }

        } catch (IOException e) {
            System.err.println("  Failed to parse Kafka message: " + e.getMessage());
        }
    }
}
