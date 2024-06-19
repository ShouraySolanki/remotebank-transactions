#!/bin/bash

SCHEMA_REGISTRY_URL="http://schema-registry:8082"
TOPIC="transactions"
SCHEMA_FILE="/usr/schema/transaction-schema.json"

# Read the schema from the file
SCHEMA=$(cat ${SCHEMA_FILE})

# Register the schema
curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data "{\"schema\": ${SCHEMA}}" \
  ${SCHEMA_REGISTRY_URL}/subjects/${TOPIC}-value/versions
