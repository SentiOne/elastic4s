package com.sksamuel.elastic4s.testkit

import com.sksamuel.exts.Logging

object DefaultJsonImplicits extends Logging {
  import com.fasterxml.jackson.databind.ObjectMapper
  import com.fasterxml.jackson.databind.node.ObjectNode
  import com.sksamuel.elastic4s._

  import scala.util.control.NonFatal

  implicit def format[T](implicit mapper: ObjectMapper = DefaultJacksonSupport.mapper,
                         manifest: Manifest[T]): JsonFormat[T] = new JsonFormat[T] {
    override def fromJson(json: String): T = {
      val t = manifest.runtimeClass.asInstanceOf[Class[T]]
      logger.debug(s"Deserializing $json to $t")
      mapper.readValue[T](json, t)
    }
  }

  implicit def JacksonJsonIndexable[T](implicit mapper: ObjectMapper = DefaultJacksonSupport.mapper): Indexable[T] = {
    new Indexable[T] {
      override def json(t: T): String = mapper.writeValueAsString(t)
    }
  }

  implicit def JacksonJsonHitReader[T](implicit mapper: ObjectMapper = DefaultJacksonSupport.mapper,
                                       manifest: Manifest[T]): HitReader[T] = new HitReader[T] {
    override def read(hit: Hit): Either[Throwable, T] = {
      require(hit.sourceAsString != null)
      try {
        val node = mapper.readTree(hit.sourceAsString).asInstanceOf[ObjectNode]
        if (!node.has("_id")) node.put("_id", hit.id)
        if (!node.has("_type")) node.put("_type", hit.`type`)
        if (!node.has("_index")) node.put("_index", hit.index)
        //  if (!node.has("_score")) node.put("_score", hit.score)
        if (!node.has("_version")) node.put("_version", hit.version)
        if (!node.has("_timestamp")) hit.sourceFieldOpt("_timestamp").collect {
          case f => f.toString
        }.foreach(node.put("_timestamp", _))
        Right(mapper.readValue[T](mapper.writeValueAsBytes(node), manifest.runtimeClass.asInstanceOf[Class[T]]))
      } catch {
        case NonFatal(e) => Left(e)
      }
    }
  }
}

object DefaultJacksonSupport {
  import com.fasterxml.jackson.annotation.JsonInclude
  import com.fasterxml.jackson.core.JsonParser
  import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
  import com.fasterxml.jackson.module.scala.DefaultScalaModule
  import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

  val mapper: ObjectMapper with ScalaObjectMapper = new ObjectMapper with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
  mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
  mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
  mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
}


