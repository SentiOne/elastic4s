package com.sksamuel.elastic4s.search.aggs

class MissingAggregationHttpTest extends AbstractAggregationTest {
  import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._

  "missing aggregation" - {
    "should return documents missing a value" in {
      val resp = client.execute {
        search in "aggregations/breakingbad" aggregations {
          aggregation missing "agg1" field "actor"
        }
      }.await
      resp.totalHits shouldBe 10
      val agg = resp.aggregations("agg1").asInstanceOf[Map[String, Integer]]("doc_count")
      agg shouldBe 7
    }
  }
}
