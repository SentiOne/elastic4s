package com.sksamuel.elastic4s.search.aggs

class ValueCountAggregationTest extends AbstractAggregationTest {
  import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._

  "value count aggregation" - {
    "should sum values for field" in {
      val resp = client.execute {
        search("aggregations/breakingbad") aggregations {
          valueCountAggregation("agg1") field "age"
        }
      }.await
      resp.totalHits shouldBe 10
      val agg = resp.aggregations("agg1").asInstanceOf[Map[String, Integer]]("value")
      agg shouldBe 10
    }
  }
}
