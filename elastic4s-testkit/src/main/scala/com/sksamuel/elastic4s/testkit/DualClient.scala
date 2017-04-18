package com.sksamuel.elastic4s.testkit

import com.sksamuel.elastic4s.embedded.LocalNode
import com.sksamuel.elastic4s.http.{HttpClient, HttpExecutable}
import com.sksamuel.elastic4s.{ElasticsearchClientUri, JsonFormat}
import org.elasticsearch.{ElasticsearchException, ElasticsearchWrapperException}
import org.scalatest._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DualClient extends SuiteMixin {
  this: Suite with DualElasticSugar =>

  var node: LocalNode = getNode
  var client: HttpClient = HttpClient(ElasticsearchClientUri("elasticsearch://" + node.ipAndPort))


  private val logger = LoggerFactory.getLogger(getClass)

  // Runs twice (once for HTTP and once for TCP)
  protected def beforeRunTests(): Unit = {
  }

  var useHttpClient = true

  def execute[T, Q1](request: T)(implicit httpExec: HttpExecutable[T, Q1], format: JsonFormat[Q1]): Future[Q1] = {
		logger.debug("Using HTTP client...")
		httpExec.execute(client.rest, request, format)
  }

  override abstract def runTests(testName: Option[String], args: Args): Status = {
    runTestsOnce(testName, args)
  }

  private def runTestsOnce(testName: Option[String], args: Args): Status = {
    try {
      beforeRunTests()
      super.runTests(testName, args)
    } finally {
      node.stop(true)
    }
  }
}
