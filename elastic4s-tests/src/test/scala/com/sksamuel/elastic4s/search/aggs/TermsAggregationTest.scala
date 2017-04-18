package com.sksamuel.elastic4s.search.aggs

class TermsAggregationTest extends AbstractAggregationTest {
  import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._

  "terms aggregation" - {
    "should group by field" in {

      val resp = client.execute {
        search("aggregations/breakingbad") aggs {
          termsAggregation("agg1").field("job")
        }
      }.await
      resp.totalHits shouldBe 10

      val buckets = getBuckets(resp)
      buckets.size shouldBe 5

      getBucketDocCountByKey(buckets, "meth kingpin") shouldBe 2
      getBucketDocCountByKey(buckets, "meth sidekick") shouldBe 3
      getBucketDocCountByKey(buckets, "dea agent") shouldBe 2
      getBucketDocCountByKey(buckets, "lawyer") shouldBe 1
      getBucketDocCountByKey(buckets, "heavy") shouldBe 2
    }

    "should only include matching documents in the query" in {
      val resp = client.execute {
        // should match 3 documents: steven, saul, hank Schrader
        search("aggregations/breakingbad") query prefixQuery("name" -> "s") aggregations {
          termsAggregation("agg1") field "job"
        }
      }.await
      resp.totalHits shouldBe 3
      val buckets = getBuckets(resp)
      buckets.size shouldBe 2
      getBucketDocCountByKey(buckets, "dea agent") shouldBe 2
      getBucketDocCountByKey(buckets, "lawyer") shouldBe 1
    }

    "should only return included fields" in {
      val resp = client.execute {
        search("aggregations/breakingbad") aggregations {
          termsAggregation("agg1") field "job" includeExclude("lawyer", "")
        }
      }.await
      resp.totalHits shouldBe 10
      val buckets = getBuckets(resp)
      buckets.size shouldBe 1
      getBucketDocCountByKey(buckets, "lawyer") shouldBe 1
    }

    "should not return excluded fields" in {
      val resp = client.execute {
        search("aggregations/breakingbad") aggregations {
          termsAggregation("agg1") field "job" includeExclude("", "lawyer")
        }
      }.await
      resp.totalHits shouldBe 10

      val buckets = getBuckets(resp)
      buckets.size shouldBe 4
      getBucketDocCountByKey(buckets, "meth sidekick") shouldBe 3
      getBucketDocCountByKey(buckets, "meth kingpin") shouldBe 2
      getBucketDocCountByKey(buckets, "dea agent") shouldBe 2
      getBucketDocCountByKey(buckets, "heavy") shouldBe 2
    }

    "should only return included fields (given a seq)" in {
      val resp = client.execute {
        search("aggregations/breakingbad") aggregations {
          termsAggregation("agg1") field "job" includeExclude(Seq("meth kingpin", "lawyer"), Nil)
        }
      }.await
      resp.totalHits shouldBe 10
      val buckets = getBuckets(resp)
      buckets.size shouldBe 2
      getBucketDocCountByKey(buckets, "meth kingpin") shouldBe 2
      getBucketDocCountByKey(buckets, "lawyer") shouldBe 1
    }

    "should not return excluded fields (given a seq)" in {
      val resp = client.execute {
        search("aggregations/breakingbad") aggregations {
          termsAggregation("agg1") field "job" includeExclude(Nil, Iterable("lawyer"))
        }
      }.await
      resp.totalHits shouldBe 10

      val buckets = getBuckets(resp)
      buckets.size shouldBe 4
      getBucketDocCountByKey(buckets, "meth sidekick") shouldBe 3
      getBucketDocCountByKey(buckets, "meth kingpin") shouldBe 2
      getBucketDocCountByKey(buckets, "dea agent") shouldBe 2
      getBucketDocCountByKey(buckets, "heavy") shouldBe 2
    }

    "should group by field and return a missing value" in {

      val resp = client.execute {
        search("aggregations/breakingbad").aggregations {
          termsAggregation("agg1") field "actor" missing "no-name"
        }
      }.await
      resp.totalHits shouldBe 10

      val buckets = getBuckets(resp)
      buckets.size shouldBe 4
      getBucketDocCountByKey(buckets, "lavell") shouldBe 1
      getBucketDocCountByKey(buckets, "bryan") shouldBe 1
      getBucketDocCountByKey(buckets, "dean") shouldBe 1
      getBucketDocCountByKey(buckets, "no-name") shouldBe 7
    }
  }
}
