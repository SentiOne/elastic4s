package com.sksamuel.elastic4s.search.aggs

class CardinalityAggregationTest extends AbstractAggregationTest {
  import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._

  "cardinality aggregation" - {
    "should count distinct values" in {
      val resp = client.execute {
        search in "aggregations/breakingbad" aggregations {
          aggregation cardinality "agg1" field "job"
        }
      }.await
      resp.totalHits shouldBe 10
      val agg = resp.aggregations("agg1").asInstanceOf[Map[String, Integer]]("value")
      agg shouldBe 5
    }
  }
}
