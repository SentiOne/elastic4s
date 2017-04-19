package com.sksamuel.elastic4s.http.search.queries.term

import com.sksamuel.elastic4s.searches.queries.term.TermsQueryDefinition
import org.elasticsearch.common.xcontent.{XContentBuilder, XContentFactory}

import scala.collection.JavaConverters._

object TermsQueryBodyFn {
  def apply(t: TermsQueryDefinition[_]): XContentBuilder = {
    val builder = XContentFactory.jsonBuilder()
    builder.startObject()
    builder.startObject("terms")
    if (t.values.nonEmpty) {
      builder.field(t.field, t.values.asJava)
    }
    t.ref.foreach { ref =>
      builder.field("index", ref.index)
      builder.field("type", ref.`type`)
      builder.field("id", ref.id)
    }
    t.path.foreach(builder.field("path", _))
    t.routing.foreach(builder.field("routing", _))
    t.boost.foreach(builder.field("boost", _))
    t.queryName.foreach(builder.field("_name", _))
    builder.endObject()
    builder.endObject()
  }
}
