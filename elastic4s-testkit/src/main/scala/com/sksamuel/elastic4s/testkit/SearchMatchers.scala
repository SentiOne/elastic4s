package com.sksamuel.elastic4s.testkit

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.searches.SearchDefinition
import org.scalatest.Matchers
import org.scalatest.matchers.{MatchResult, Matcher}

import scala.concurrent.duration._

trait SearchMatchers extends Matchers {
  import DefaultJsonImplicits._

  @deprecated("use containId(id)", "5.0.0")
  def containResult(expectedId: Any)
                   (implicit client: HttpClient, timeout: FiniteDuration = 10.seconds): Matcher[SearchDefinition] = {
    containId(expectedId)
  }

  def containId(expectedId: Any)
               (implicit client: HttpClient,
                timeout: FiniteDuration = 10.seconds): Matcher[SearchDefinition] = new Matcher[SearchDefinition] {
    override def apply(left: SearchDefinition): MatchResult = {
      val resp = client.execute(left).await(timeout)
      val exists = resp.hits.hits.exists(_.id == expectedId.toString)
      MatchResult(
        exists,
        s"Search $left did not find document $expectedId",
        s"Search $left found document $expectedId"
      )
    }
  }

  def haveFieldValue(value: String)
                    (implicit client: HttpClient,
                     timeout: FiniteDuration = 10.seconds): Matcher[SearchDefinition] = new
      Matcher[SearchDefinition] {
    override def apply(left: SearchDefinition): MatchResult = {
      val resp = client.execute(left).await(timeout)
      val exists = resp.hits.hits.map(_.fields)
        .flatMap(_.flatMap(_._2 match {
          case values: Seq[AnyRef] => values
          case value: AnyRef => Seq(value)
        }))
        .contains(value)

      MatchResult(
        exists,
        s"Search ${left.indexesTypes.indexes.mkString(",")}/${left.indexesTypes.types.mkString(",")} did not contain field value '$value'",
        s"Search ${left.indexesTypes.indexes.mkString(",")}/${left.indexesTypes.types.mkString(",")} contained unwanted field value $value"
      )
    }
  }

  def haveSourceField(value: String)
                     (implicit client: HttpClient,
                      timeout: FiniteDuration = 10.seconds): Matcher[SearchDefinition] = new Matcher[SearchDefinition] {
    override def apply(left: SearchDefinition): MatchResult = {
      val resp = client.execute(left).await(timeout)
      val exists = resp.hits.hits.exists(_.sourceAsMap.contains(value))
      MatchResult(
        exists,
        s"Search $left contained unwanted source field $value",
        s"Search $left did not contain unwanted source field $value"
      )
    }
  }

  def haveSourceFieldValue(field: String, value: String)
                          (implicit client: HttpClient,
                           timeout: FiniteDuration = 10.seconds): Matcher[SearchDefinition] = new
      Matcher[SearchDefinition] {
    override def apply(left: SearchDefinition): MatchResult = {
      val resp = client.execute(left).await(timeout)
      val exists = resp.hits.hits.flatMap(_.sourceAsMap.get(field)).contains(value)
      MatchResult(
        exists,
        s"Search $left contained unwanted source field value $value",
        s"Search $left did not contain unwanted source field value $value"
      )
    }
  }

  def haveTotalHits(expectedCount: Int)
                   (implicit client: HttpClient,
                    timeout: FiniteDuration = 10.seconds): Matcher[SearchDefinition] = new Matcher[SearchDefinition] {
    override def apply(left: SearchDefinition): MatchResult = {
      val resp = client.execute(left).await(timeout)
      val count = resp.totalHits
      MatchResult(
        count == expectedCount,
        s"Search $left found $count totalHits",
        s"Search $left found $count totalHits"
      )
    }
  }

  def haveHits(expectedCount: Int)
              (implicit client: HttpClient,
               timeout: FiniteDuration = 10.seconds): Matcher[SearchDefinition] = new Matcher[SearchDefinition] {
    override def apply(left: SearchDefinition): MatchResult = {
      val resp = client.execute(left).await(timeout)
      val count = resp.hits.size
      MatchResult(
        count == expectedCount,
        s"Search $left found $count hits",
        s"Search $left found $count hits"
      )
    }
  }

  def haveNoHits(implicit client: HttpClient,
                 timeout: FiniteDuration = 10.seconds): Matcher[SearchDefinition] = new Matcher[SearchDefinition] {
    override def apply(left: SearchDefinition): MatchResult = {
      val resp = client.execute(left).await(timeout)
      val count = resp.hits.size
      MatchResult(
        count == 0,
        s"Search $left found $count hits",
        s"Search $left found $count hits"
      )
    }
  }
}
