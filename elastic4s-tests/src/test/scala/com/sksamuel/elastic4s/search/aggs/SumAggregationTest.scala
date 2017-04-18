package com.sksamuel.elastic4s.search.aggs

class SumAggregationTest extends AbstractAggregationTest {
  import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._

  "sum aggregation" - {
    "should sum values for field" in {
      val resp = client.execute {
        search in "aggregations/breakingbad" aggregations {
          aggregation sum "agg1" field "age"
        }
      }.await
      resp.totalHits shouldBe 10
      val agg = resp.aggregations("agg1").asInstanceOf[Map[String, Double]]("value")
      agg shouldBe 454.0
    }
  }
}
