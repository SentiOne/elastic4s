package com.sksamuel.elastic4s.http.search.queries.specialized

import com.sksamuel.elastic4s.DocumentRef
import com.sksamuel.elastic4s.searches.queries.PercolateQueryDefinition
import org.elasticsearch.common.bytes.BytesArray
import org.elasticsearch.common.xcontent.{XContentBuilder, XContentFactory, XContentType}

object PercolateQueryBodyFn {
  def apply(q: PercolateQueryDefinition): XContentBuilder = {
    val builder = XContentFactory.jsonBuilder()
    builder.startObject()
    builder.startObject("percolate")

    builder.field("field", q.field)
    builder.field("document_type", q.`type`)


    q.ref match {
      case Some(DocumentRef(docIndex, docType, docId)) =>
        builder.field("index", docIndex)
        builder.field("type", docType)
        builder.field("id", docId)
      case _ =>
        q.source.fold(sys.error("Must specify id or source")) { src =>
          builder.rawField("document", new BytesArray(src), XContentType.JSON)
        }
    }

    builder.endObject()
    builder.endObject()
    builder
  }
}
