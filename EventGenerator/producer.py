from confluent_kafka import Producer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.json_schema import JSONSerializer
from confluent_kafka.serialization import SerializationContext, MessageField
import json
import time
import random

schema_registry_url = 'http://schema-registry:8082'
schema_path = 'schema/transaction-schema.json'

# Load schema from file
with open(schema_path, 'r') as file:
    schema_str = file.read()

schema_registry_client = SchemaRegistryClient({'url': schema_registry_url})
json_serializer = JSONSerializer(schema_str, schema_registry_client)

producer_conf = {
    'bootstrap.servers': 'kafka:9092'
}

producer = Producer(producer_conf)

def generate_fake_transaction(user_id):
    return {
        "user_id": user_id,
        "transaction_timestamp_millis": int(time.time() * 1000),
        "amount": round(random.uniform(-1000, 1000), 2),
        "currency": "USD",
        "counterpart_id": random.randint(1, 1000)
    }

def delivery_report(err, msg):
    if err is not None:
        print(f"Message delivery failed: {err}")
    else:
        print(f"Message delivered to {msg.topic()} [{msg.partition()}]")

user_ids = range(1, 101)
while True:
    for user_id in user_ids:
        transaction = generate_fake_transaction(user_id)
        serialized_transaction = json_serializer(transaction, SerializationContext('transactions', MessageField.VALUE))
        producer.produce('transactions', key=str(user_id), value=serialized_transaction, callback=delivery_report)
    producer.poll(0)
    time.sleep(1)