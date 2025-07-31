# Policy Service for Autocomplete Suggestions

This microservice acts as a Kafka consumer and Redis proxy that applies filtering policies to autocomplete suggestions. It ensures that sensitive or restricted terms (e.g., offensive or banned keywords) do not make it into Redis or the serving layer.

---

##  Overview

The Policy Service listens to a Kafka topic for incoming autocomplete suggestions and checks each prefix against a list of forbidden keywords. If the key is clean, the corresponding suggestion is saved to Redis. If the key contains any restricted word, it is skipped silently.

---

##  Responsibilities

* Subscribe to a Kafka topic that contains new autocomplete suggestions
* Deserialize and validate each suggestion
* Apply keyword filtering policy
* Sync clean suggestions into Redis Cluster

---

##  Tech Stack

* Java 17
* Spring Boot
* Spring Kafka
* Redis Cluster (Spring Data Redis)

---

##  Project Structure

```text
src/
├── config/
│   └── RedisConfig.java                   # Redis cluster configuration
├── model/
│   └── PrefixSuggestion.java             # POJO for incoming suggestion messages
├── policy/
│   └── AutoCompletePolicy.java           # Filtering logic based on blocked keywords
├── listener/
│   └── KafkaSuggestionListener.java      # Kafka listener and processing logic
├── PolicyServiceApplication.java         # Application entry point
└── resources/
    └── application.properties            # Kafka/Redis/Filter configuration
```

---

##  Running the Service

### Using Maven:

```bash
./mvnw spring-boot:run
```

### Building a Docker image:

```bash
./mvnw clean package -DskipTests
docker build -t policy-service:latest .
```

---

##  Configuration (`application.properties`)

```properties
# Kafka
spring.kafka.bootstrap-servers=kafka-service:9092
spring.kafka.consumer.group-id=policy-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer

# Redis Cluster
spring.data.redis.cluster.nodes=redis-redis-cluster-0.redis-redis-cluster-headless:6379,redis-redis-cluster-1.redis-redis-cluster-headless:6379,redis-redis-cluster-2.redis-redis-cluster-headless:6379
spring.data.redis.cluster.max-redirects=3

# Filtered keywords (comma-separated)
filter.keywords=password,shadi,bashar,mofeed
```

You can customize the `filter.keywords` list to apply your own restriction policies.

---

##  Message Flow

```text
Kafka Topic → KafkaSuggestionListener → AutoCompletePolicy → Redis Cluster
```

* KafkaSuggestionListener receives `PrefixSuggestion` messages
* AutoCompletePolicy checks the prefix against the blocked list
* If clean → write to Redis; otherwise → ignore

---

##  Filtering Policy

Filtering is based on simple containment. Any prefix that includes one of the blocked keywords will be dropped.

Example:

* ✅ Allowed: `flower`, `travel to paris`
* ❌ Blocked: `shadi events`, `my password123`

---

##  Kubernetes Deployment

### ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: policy-service-config
  namespace: kh-pipeline
  labels:
    app: policy-service
data:
  application.properties: |
    # Kafka
    spring.kafka.bootstrap-servers=kafka-service:9092
    spring.kafka.consumer.group-id=policy-group
    spring.kafka.consumer.auto-offset-reset=earliest
    spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
    spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer

    # Redis Cluster
    spring.data.redis.cluster.nodes=\
    redis-redis-cluster-0.redis-redis-cluster-headless:6379,\
    redis-redis-cluster-1.redis-redis-cluster-headless:6379,\
    redis-redis-cluster-2.redis-redis-cluster-headless:6379
    spring.data.redis.cluster.max-redirects=3
```

### Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: policy-service
  namespace: kh-pipeline
spec:
  replicas: 1
  selector:
    matchLabels:
      app: policy-service
  template:
    metadata:
      labels:
        app: policy-service
    spec:
      containers:
        - name: policy-service
          image: 172.29.3.41:5000/policy_service:1.4
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8096
          volumeMounts:
            - name: app-props
              mountPath: /workspace/config
              readOnly: true
          env:
            - name: SPRING_CONFIG_ADDITIONAL_LOCATION
              value: file:/workspace/config/
            - name: SPRING_DATA_REDIS_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: redis-redis-cluster
                  key: redis-password
      volumes:
        - name: app-props
          configMap:
            name: policy-service-config
```

---

## 📄 License

kheder khdrhswn32@gmail.com 
