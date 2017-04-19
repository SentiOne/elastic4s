package com.sksamuel.elastic4s.http.search

import cats.Show
import com.sksamuel.elastic4s.JsonFormat
import com.sksamuel.elastic4s.http.HttpExecutable
import com.sksamuel.elastic4s.searches.queries.term.{BuildableTermsQuery, TermsQueryDefinition}
import com.sksamuel.elastic4s.searches.{MultiSearchDefinition, SearchDefinition}
import org.apache.http.entity.{ContentType, StringEntity}
import org.elasticsearch.client.RestClient

import scala.collection.JavaConverters._
import scala.concurrent.Future

trait SearchImplicits {

  implicit def BuildableTermsNoOp[T] = new BuildableTermsQuery[T] {
    override def build(q: TermsQueryDefinition[T]): Any = null // not used by the http builders
  }

  implicit object SearchShow extends Show[SearchDefinition] {
    override def show(req: SearchDefinition): String = SearchBodyBuilderFn(req).string()
  }

  implicit object MultiSearchShow extends Show[MultiSearchDefinition] {
    override def show(req: MultiSearchDefinition): String = MultiSearchContentBuilder(req)
  }

  implicit object MultiSearchHttpExecutable extends HttpExecutable[MultiSearchDefinition, MultiSearchResponse] {

    override def execute(client: RestClient,
                         request: MultiSearchDefinition,
                         format: JsonFormat[MultiSearchResponse]): Future[MultiSearchResponse] = {

      val params = scala.collection.mutable.Map.empty[String, String]
      request.maxConcurrentSearches.map(_.toString).foreach(params.put("max_concurrent_searches", _))

      val body = MultiSearchContentBuilder(request)
      logger.debug("Executing msearch: " + body)
      val entity = new StringEntity(body, ContentType.APPLICATION_JSON)

      executeAsyncAndMapResponse(client.performRequestAsync("POST", "/_msearch", params.asJava, entity, _), format)
    }
  }

  implicit object SearchHttpExecutable extends HttpExecutable[SearchDefinition, SearchResponse] {

    override def execute(client: RestClient,
                         request: SearchDefinition,
                         format: JsonFormat[SearchResponse]): Future[SearchResponse] = {

      val endpoint = if (request.indexesTypes.indexes.isEmpty && request.indexesTypes.types.isEmpty)
        "/_search"
      else if (request.indexesTypes.indexes.isEmpty)
        "/_all/" + request.indexesTypes.types.mkString(",") + "/_search"
      else if (request.indexesTypes.types.isEmpty)
        "/" + request.indexesTypes.indexes.mkString(",") + "/_search"
      else
        "/" + request.indexesTypes.indexes.mkString(",") + "/" + request.indexesTypes.types.mkString(",") + "/_search"

      val params = scala.collection.mutable.Map.empty[String, String]
      request.from.map(_.toString).foreach(params.put("from", _))
      request.keepAlive.foreach(params.put("scroll", _))
      request.pref.foreach(params.put("preference", _))
      request.requestCache.map(_.toString).foreach(params.put("request_cache", _))
      request.routing.foreach(params.put("routing", _))
      request.size.map(_.toString).foreach(params.put("size", _))
      request.searchType.map(_.toString).foreach(params.put("search_type", _))
      request.terminateAfter.map(_.toString).foreach(params.put("terminate_after", _))
      request.timeout.map(_.toMillis + "ms").foreach(params.put("timeout", _))
      request.version.map(_.toString).foreach(params.put("version", _))

      val builder = SearchBodyBuilderFn(request)
      logger.debug("Executing search request: " + builder.string)

      val body = builder.string()
      val entity = new StringEntity(body, ContentType.APPLICATION_JSON)

      executeAsyncAndMapResponse(client.performRequestAsync("POST", endpoint, params.asJava, entity, _), format)
    }
  }

  implicit class SearchDefinitionShowOps(f: SearchDefinition) {
    def show: String = SearchShow.show(f)
  }

  implicit object MultiSearchDefinitionShow extends Show[MultiSearchDefinition] {
    import compat.Platform.EOL
    override def show(f: MultiSearchDefinition): String = f.searches.map(_.show).mkString("[" + EOL, "," + EOL, "]")
  }

  implicit class MultiSearchDefinitionShowOps(f: MultiSearchDefinition) {
    def show: String = MultiSearchDefinitionShow.show(f)
  }
}
