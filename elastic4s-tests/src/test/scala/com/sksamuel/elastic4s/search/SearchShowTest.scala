package com.sksamuel.elastic4s.search

import com.sksamuel.elastic4s.testkit.ElasticSugar
import org.scalatest.{Matchers, WordSpec}

class SearchShowTest extends WordSpec with Matchers with ElasticSugar {

  "Search" should {
    "have a show typeclass implementation" in {

      val request = {
        search("gameofthrones" / "characters") query {
          boolQuery().
            should(
              termQuery("name", "snow")
            ).must(
            matchQuery("location", "the wall")
          )
        }
      }

      request.show.trim shouldBe
        """{
          |  "query" : {
          |    "bool" : {
          |      "must" : [
          |        {
          |          "match" : {
          |            "location" : {
          |              "query" : "the wall"
          |            }
          |          }
          |        }
          |      ],
          |      "should" : [
          |        {
          |          "term" : {
          |            "name" : {
          |              "value" : "snow"
          |            }
          |          }
          |        }
          |      ]
          |    }
          |  }
          |}""".stripMargin.trim.replaceAll("\n\\s*", "").replaceAll(" : ", ":")
    }
  }
}
