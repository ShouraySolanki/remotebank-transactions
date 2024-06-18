#!/bin/bash

# Function to create a Kafka topic
create_topic() {
  local topic=$1
  local partitions=$2
  local replication_factor=$3
  local retention_ms=$4

  kafka-topics --create --if-not-exists --bootstrap-server kafka:9092 \
    --replication-factor "$replication_factor" \
    --partitions "$partitions" \
    --topic "$topic" \
    --config retention.ms="$retention_ms"

  if [ $? -eq 0 ]; then
    echo "Topic '$topic' created successfully."
  else
    echo "Failed to create topic '$topic'."
  fi
}

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
while ! nc -z kafka 9092; do
  sleep 0.1
done
echo "Kafka is ready!"

# Define the topics to create
TOPICS=("transactions")
PARTITIONS=3
REPLICATION_FACTOR=1
RETENTION_MS=86400000  # 1 day

# Create each topic
for TOPIC in "${TOPICS[@]}"; do
  create_topic "$TOPIC" "$PARTITIONS" "$REPLICATION_FACTOR" "$RETENTION_MS"
done
