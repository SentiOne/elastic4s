package com.sksamuel.elastic4s.testkit

import com.sksamuel.elastic4s.embedded.LocalNode
import com.sksamuel.elastic4s.http.index.RefreshIndexResponse
import com.sksamuel.elastic4s.http.{ElasticDsl, HttpClient}
import com.sksamuel.elastic4s.{ElasticsearchClientUri, IndexAndTypes, Indexes}
import org.elasticsearch.ResourceAlreadyExistsException
import org.elasticsearch.cluster.health.ClusterHealthStatus
import org.elasticsearch.transport.RemoteTransportException
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.slf4j.LoggerFactory
import ResponseConverterImplicits._

trait ElasticSugar extends AbstractElasticSugar with ClassLocalNodeProvider with BeforeAndAfterAll {
  this: Suite with LocalNodeProvider =>

  implicit val node = getNode
  implicit val client = HttpClient(ElasticsearchClientUri("elasticsearch://" + node.ipAndPort))

  override def afterAll(): Unit = {
    node.stop(true)
  }
}

trait SharedElasticSugar extends AbstractElasticSugar with ClassloaderLocalNodeProvider {
  this: Suite with LocalNodeProvider =>

  implicit val node = getNode
  implicit val client = HttpClient(ElasticsearchClientUri("elasticsearch://" + node.ipAndPort))
}

trait DualElasticSugar extends AbstractElasticSugar with AlwaysNewLocalNodeProvider {
  this: Suite with LocalNodeProvider =>
}

/**
  * Provides helper methods for things like refreshing an index, and blocking until an
  * index has a certain count of documents. These methods are very useful when writing
  * tests to allow for blocking, iterative coding
  */
trait AbstractElasticSugar extends ElasticDsl {
  this: Suite with LocalNodeProvider =>
  def node: LocalNode
  def client: HttpClient
  private val logger = LoggerFactory.getLogger(getClass)
  import DefaultJsonImplicits._

  // refresh all indexes
  def refreshAll(): RefreshIndexResponse = refresh(Indexes.All)

  // refreshes all specified indexes
  def refresh(indexes: Indexes): RefreshIndexResponse = {
    client.execute {
      refreshIndex(indexes)
    }.await
  }

  def blockUntilGreen(): Unit = {
    blockUntil("Expected cluster to have green status") { () =>
      val status = client.execute {
        clusterHealth()
      }.await.status
      ClusterHealthStatus.fromString(status) == ClusterHealthStatus.GREEN
    }
  }

  def blockUntil(explain: String)(predicate: () => Boolean): Unit = {

    var backoff = 0
    var done = false

    while (backoff <= 16 && !done) {
      if (backoff > 0) Thread.sleep(200 * backoff)
      backoff = backoff + 1
      try {
        done = predicate()
      } catch {
        case e: Throwable =>
          logger.warn("problem while testing predicate", e)
      }
    }

    require(done, s"Failed waiting on: $explain")
  }

  def ensureIndexExists(index: String): Unit = {
    try {
      client.execute {
        createIndex(index)
      }.await
    } catch {
      case _: ResourceAlreadyExistsException => // Ok, ignore.
      case _: RemoteTransportException => // Ok, ignore.
    }
  }

  def doesIndexExists(name: String): Boolean = {
    client.execute {
      indexExists(name)
    }.await.isExists
  }

  def deleteIndex(name: String): Unit = {
    if (doesIndexExists(name)) {
      client.execute {
        ElasticDsl.deleteIndex(name)
      }.await
    }
  }

  def truncateIndex(index: String): Unit = {
    deleteIndex(index)
    ensureIndexExists(index)
    blockUntilEmpty(index)
  }

  def blockUntilDocumentExists(id: String, index: String, `type`: String): Unit = {
    blockUntil(s"Expected to find document $id") { () =>
      client.execute {
        get(id).from(index / `type`)
      }.await.exists
    }
  }

  def blockUntilCount(expected: Long, index: String): Unit = {
    blockUntil(s"Expected count of $expected") { () =>
      val result = client.execute {
        search(index).matchAllQuery().size(0)
      }.await
      expected <= result.totalHits
    }
  }

  def blockUntilCount(expected: Long, indexAndTypes: IndexAndTypes): Unit = {
    blockUntil(s"Expected count of $expected") { () =>
      val result = client.execute {
        search(indexAndTypes).matchAllQuery().size(0)
      }.await
      expected <= result.totalHits
    }
  }

  /**
    * Will block until the given index and optional types have at least the given number of documents.
    */
  def blockUntilCount(expected: Long, index: String, types: String*): Unit = {
    blockUntil(s"Expected count of $expected") { () =>
      val result = client.execute {
        search(index / types).matchAllQuery().size(0)
      }.await
      expected <= result.totalHits
    }
  }

  def blockUntilExactCount(expected: Long, index: String, types: String*): Unit = {
    blockUntil(s"Expected count of $expected") { () =>
      expected == client.execute {
        search(index / types).size(0)
      }.await.totalHits
    }
  }

  def blockUntilEmpty(index: String): Unit = {
    blockUntil(s"Expected empty index $index") { () =>
      client.execute {
        search(Indexes(index)).size(0)
      }.await.totalHits == 0
    }
  }

  def blockUntilIndexExists(index: String): Unit = {
    blockUntil(s"Expected exists index $index") { () ⇒
      doesIndexExists(index)
    }
  }

  def blockUntilIndexNotExists(index: String): Unit = {
    blockUntil(s"Expected not exists index $index") { () ⇒
      !doesIndexExists(index)
    }
  }

  def blockUntilDocumentHasVersion(index: String, `type`: String, id: String, version: Long): Unit = {
    blockUntil(s"Expected document $id to have version $version") { () =>
      client.execute {
        get(id).from(index / `type`)
      }.await.version == version
    }
  }
}
