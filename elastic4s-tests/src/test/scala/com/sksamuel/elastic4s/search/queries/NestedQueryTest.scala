package com.sksamuel.elastic4s.search.queries

import com.sksamuel.elastic4s.testkit.SharedElasticSugar
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.scalatest.{Matchers, WordSpec}

class NestedQueryTest extends WordSpec with SharedElasticSugar with Matchers {
  import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._

  client.execute {
    createIndex("nested").mappings(
      mapping("places").fields(
        keywordField("name"),
        nestedField("states")
      )
    )
  }

  client.execute(
    bulk(
      indexInto("nested" / "places") fields(
        "name" -> "usa",
        "states" -> Seq(
          Map(
            "name" -> "Montana",
            "capital" -> "Helena",
            "entry" -> 1889
          ), Map(
            "name" -> "South Dakota",
            "capital" -> "Pierre",
            "entry" -> 1889
          )
        )
      ),
      indexInto("nested" / "places") fields(
        "name" -> "fictional usa",
        "states" -> Seq(
          Map(
            "name" -> "Old Jersey",
            "capital" -> "Trenton",
            "entry" -> 1889
          ), Map(
            "name" -> "Montana",
            "capital" -> "Helena",
            "entry" -> 1567
          )
        )
      )
    ).refresh(RefreshPolicy.IMMEDIATE)
  ).await

  "nested query" should {
    "match against nested objects" in {
      client.execute {
        search("nested" / "places") query {
          nestedQuery("states").query {
            boolQuery.must(
              matchQuery("states.name", "Montana"),
              matchQuery("states.entry", 1889)
            )
          }
        }
      }.await.totalHits shouldBe 1
    }
  }
}
