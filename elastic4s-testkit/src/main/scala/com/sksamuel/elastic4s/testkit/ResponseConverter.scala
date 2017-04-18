package com.sksamuel.elastic4s.testkit

import java.util
import java.util.Locale

import com.sksamuel.elastic4s.http.Shards
import com.sksamuel.elastic4s.http.delete.DeleteByQueryResponse
import com.sksamuel.elastic4s.http.index._
import com.sksamuel.elastic4s.http.validate.ValidateResponse
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheResponse
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsResponse
import org.elasticsearch.action.admin.indices.flush.FlushResponse
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse
import org.elasticsearch.action.admin.indices.validate.query.ValidateQueryResponse
import org.elasticsearch.action.bulk.byscroll.{BulkByScrollResponse, BulkByScrollTask}
import scala.collection.JavaConverters._

trait ResponseConverter[T, R] {
  def convert(response: T): R
}

object ResponseConverterImplicits {

  import com.sksamuel.elastic4s.http.search.SearchResponse

  implicit object FlushIndexResponseConverter extends ResponseConverter[FlushResponse, FlushIndexResponse] {
    override def convert(response: FlushResponse) = FlushIndexResponse(
      Shards(response.getTotalShards, response.getFailedShards, response.getSuccessfulShards)
    )
  }

  implicit object IndexExistsResponseConverter extends ResponseConverter[IndicesExistsResponse, IndexExistsResponse] {
    override def convert(response: IndicesExistsResponse) = IndexExistsResponse(response.isExists)
  }

  implicit object RefreshIndexResponseConverter extends ResponseConverter[RefreshResponse, RefreshIndexResponse] {
    override def convert(response: RefreshResponse) = RefreshIndexResponse()
  }

  implicit object TypeExistsResponseConverter extends ResponseConverter[TypesExistsResponse, TypeExistsResponse] {
    override def convert(response: TypesExistsResponse) = TypeExistsResponse(response.isExists)
  }

  implicit object ClearCacheResponseConverter extends ResponseConverter[ClearIndicesCacheResponse, ClearCacheResponse] {
    override def convert(response: ClearIndicesCacheResponse) = ClearCacheResponse(
      Shards(
        response.getTotalShards,
        response.getFailedShards,
        response.getSuccessfulShards
      )
    )
  }

  implicit object DeleteByQueryResponseConverter extends ResponseConverter[BulkByScrollResponse, DeleteByQueryResponse] {
    override def convert(response: BulkByScrollResponse) = {
      val field = classOf[BulkByScrollResponse].getDeclaredField("status")
      field.setAccessible(true)
      val status = field.get(response).asInstanceOf[BulkByScrollTask.Status]

      DeleteByQueryResponse(
        response.getTook.millis,
        response.isTimedOut,
        status.getTotal,
        response.getDeleted,
        response.getBatches,
        response.getVersionConflicts,
        response.getNoops,
        status.getThrottled.millis,
        if(status.getRequestsPerSecond == Float.PositiveInfinity) -1 else status.getRequestsPerSecond.toLong,
        status.getThrottledUntil.millis
      )
    }
  }

  implicit object ValidateResponseConverter extends ResponseConverter[ValidateQueryResponse, ValidateResponse] {
    import com.sksamuel.elastic4s.http.validate.Explanation

    override def convert(response: ValidateQueryResponse) = ValidateResponse(
      response.isValid,
      Shards(
        response.getTotalShards,
        response.getFailedShards,
        response.getSuccessfulShards
      ),
      response.getQueryExplanation.asScala.map(x => Explanation(x.getIndex, x.isValid, x.getError))
    )
  }

  implicit class RichSourceMap(val self: Map[String, AnyRef]) extends AnyVal {
    def asScalaNested: Map[String, AnyRef] = {
      def toScala(a: AnyRef): AnyRef = a match {
        case i: java.lang.Iterable[AnyRef] => i.asScala.map(toScala)
        case m: util.Map[AnyRef, AnyRef] => m.asScala.map { case (k, v) => (toScala(k), toScala(v)) }
        case other => other
      }

      self.mapValues(toScala)
    }
  }

}
