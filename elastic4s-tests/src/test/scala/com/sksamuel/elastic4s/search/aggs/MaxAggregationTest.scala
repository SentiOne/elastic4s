package com.sksamuel.elastic4s.search.aggs

class MaxAggregationTest extends AbstractAggregationTest {
  import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._

  "max aggregation" - {
    "should count max value for field" in {
      val resp = client.execute {
        search in "aggregations/breakingbad" aggregations {
          maxAggregation("agg1") field "age"
        }
      }.await
      resp.totalHits shouldBe 10
      val agg = resp.aggregations("agg1").asInstanceOf[Map[String, Double]]("value")
      agg shouldBe 60
    }
  }
}
