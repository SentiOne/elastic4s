package com.sksamuel.elastic4s.search.aggs

import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.testkit.ElasticSugar
import org.scalatest.{FreeSpec, Matchers}

abstract class AbstractAggregationTest extends FreeSpec with Matchers with ElasticSugar {
  import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._

  client.execute {
    createIndex("aggregations") mappings {
      mapping("breakingbad") fields(
        keywordField("job"),
        keywordField("actor")
      )
    }
  }.await

  client.execute(
    bulk(
      indexInto("aggregations/breakingbad") fields("name" -> "walter white", "job" -> "meth kingpin", "age" -> 50, "actor" -> "bryan"),
      indexInto("aggregations/breakingbad") fields("name" -> "hank schrader", "job" -> "dea agent", "age" -> 55, "actor" -> "dean"),
      indexInto("aggregations/breakingbad") fields("name" -> "jesse pinkman", "job" -> "meth sidekick", "age" -> 30),
      indexInto("aggregations/breakingbad") fields("name" -> "gus fring", "job" -> "meth kingpin", "age" -> 60),
      indexInto("aggregations/breakingbad") fields("name" -> "steven gomez", "job" -> "dea agent", "age" -> 50),
      indexInto("aggregations/breakingbad") fields("name" -> "saul goodman", "job" -> "lawyer", "age" -> 55),
      indexInto("aggregations/breakingbad") fields("name" -> "Huell Babineaux", "job" -> "heavy", "age" -> 43, "actor" -> "lavell"),
      indexInto("aggregations/breakingbad") fields("name" -> "mike ehrmantraut", "job" -> "heavy", "age" -> 45),
      indexInto("aggregations/breakingbad") fields("name" -> "lydia rodarte quayle", "job" -> "meth sidekick", "age" -> 40),
      indexInto("aggregations/breakingbad") fields("name" -> "todd alquist", "job" -> "meth sidekick", "age" -> 26)
    )
  ).await

  refresh("aggregations")
  blockUntilCount(10, "aggregations")

  protected def getBuckets(searchResponse: SearchResponse, aggregationName: String = "agg1"): List[Map[String, AnyRef]] = {
    val agg = searchResponse.aggregations(aggregationName).asInstanceOf[Map[String, AnyRef]]
    agg("buckets").asInstanceOf[List[Map[String, AnyRef]]]
  }

  protected def getBucketDocCountByKey[T](buckets: List[Map[String, AnyRef]], key: T) : Integer = {
    buckets
      .find(bucket => bucket("key") == key)
      .map(bucket => bucket("doc_count").asInstanceOf[Integer]).getOrElse(0)
  }
}
