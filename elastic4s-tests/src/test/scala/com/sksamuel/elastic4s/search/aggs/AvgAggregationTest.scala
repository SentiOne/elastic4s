package com.sksamuel.elastic4s.search.aggs

class AvgAggregationTest extends AbstractAggregationTest {
  import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._

  "avg aggregation" - {
    "should average by field" in {
      val resp = client.execute {
        search("aggregations/breakingbad") aggregations {
          aggregation avg "agg1" field "age"
        }
      }.await
      resp.totalHits shouldBe 10
      val agg = resp.aggregations("agg1").asInstanceOf[Map[String, Double]]("value")
      agg shouldBe 45.4
    }
    "should only include matching documents in the query" in {
      val resp = client.execute {
        // should match 3 documents
        search("aggregations/breakingbad") query prefixQuery("name" -> "g") aggregations {
          aggregation avg "agg1" field "age"
        }
      }.await
      resp.totalHits shouldBe 3
      val agg = resp.aggregations("agg1").asInstanceOf[Map[String, Double]]("value")
      agg shouldBe 55
    }
  }
}
