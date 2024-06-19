package com.remotebank.transactions

import io.confluent.kafka.serializers.json.{KafkaJsonSchemaDeserializer, KafkaJsonSchemaDeserializerConfig}
import org.apache.flink.api.common.serialization.DeserializationSchema
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.TypeExtractor

import java.util

class CustomJsonSchemaDeserializationSchema extends DeserializationSchema[util.Map[String, Any]] {
  @transient private var deserializer: KafkaJsonSchemaDeserializer[util.Map[String, Any]] = _

  override def deserialize(message: Array[Byte]): util.Map[String, Any] = {
    if (deserializer == null) {
      val properties = new util.HashMap[String, Any]()
      properties.put("schema.registry.url", "http://schema-registry:8082")
      properties.put(KafkaJsonSchemaDeserializerConfig.JSON_VALUE_TYPE, classOf[util.Map[String, Any]].getName)
      deserializer = new KafkaJsonSchemaDeserializer[util.Map[String, Any]]()
      deserializer.configure(properties, false)
    }
    deserializer.deserialize("", message)
  }

  override def isEndOfStream(nextElement: util.Map[String, Any]): Boolean = false

  override def getProducedType: TypeInformation[util.Map[String, Any]] = {
    TypeExtractor.getForClass(classOf[util.Map[String, Any]])
  }
}
