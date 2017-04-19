package com.sksamuel.elastic4s.http.search

import java.util

import com.sksamuel.elastic4s.http.search.aggs.AggregationBuilderFn
import com.sksamuel.elastic4s.http.search.queries.{QueryBuilderFn, SortContentBuilder}
import com.sksamuel.elastic4s.searches.SearchDefinition
import com.sksamuel.elastic4s.searches.suggestion.{CompletionSuggestionDefinition, PhraseSuggestionDefinition, TermSuggestionDefinition}
import org.elasticsearch.common.bytes.BytesArray
import org.elasticsearch.common.xcontent.{ToXContent, XContentBuilder, XContentFactory, XContentType}

import scala.collection.JavaConverters._

object SearchBodyBuilderFn {

  def apply(request: SearchDefinition): XContentBuilder = {

    val builder = XContentFactory.jsonBuilder()
    builder.startObject()

    request.query.map(QueryBuilderFn.apply).foreach(x => builder.rawField("query", new BytesArray(x.string), XContentType.JSON))
    request.postFilter.map(QueryBuilderFn.apply).foreach(x => builder.rawField("post_filter", new BytesArray(x.string), XContentType.JSON))

    if (request.explain.contains(true)) {
      builder.field("explain", true)
    }

    request.minScore.foreach(builder.field("min_score", _))
    if (request.searchAfter.nonEmpty) {
      builder.field("search_after", request.searchAfter.asJava)
    }

    if (request.sorts.nonEmpty) {
      builder.startArray("sort")
      // Workaround for bug where separator is not added with rawValues
      val arrayBody = new BytesArray(request.sorts.map(s => SortContentBuilder(s).string).mkString(","))
      builder.rawValue(arrayBody, XContentType.JSON)
      builder.endArray()
    }

    if (request.highlight.nonEmpty) {
      builder.startObject("highlight")
      request.highlight.foreach { highlight =>
        if (highlight.options.preTags.nonEmpty) {
          builder.array("pre_tags", highlight.options.preTags : _*)
        }
        if (highlight.options.postTags.nonEmpty) {
          builder.array("post_tags", highlight.options.postTags : _*)
        }
        highlight.options.boundaryChars.foreach(builder.field("boundary_chars", _))
        highlight.options.boundaryMaxScan.foreach(builder.field("boundary_max_scan", _))
        highlight.options.order.foreach(builder.field("order", _))
        highlight.options.encoder.foreach(builder.field("encoder", _))
        builder.startObject("fields")
        highlight.fields.foreach { field =>
          builder.startObject(field.field)
          field.boundaryChars.foreach(builder.field("boundary_chars", _))
          field.boundaryMaxScan.foreach(builder.field("boundary_max_scan", _))
          field.forceSource.foreach(builder.field("force_source", _))
          field.fragmentOffset.foreach(builder.field("fragment_offset", _))
          field.fragmentSize.foreach(builder.field("fragment_size", _))
          field.highlightQuery.map(QueryBuilderFn.apply).map(_.bytes()).foreach { highlight =>
            builder.rawField("highlight_query", highlight, XContentType.JSON)
          }
          field.matchedFields.foreach(builder.field("matched_fields", _))
          field.noMatchSize.foreach(builder.field("no_match_size", _))
          field.numOfFragments.foreach(builder.field("number_of_fragments", _))
          field.highlighterType.foreach(builder.field("type", _))
          field.order.foreach(builder.field("order", _))
          field.phraseLimit.foreach(builder.field("phrase_limit", _))
          if (field.postTags.nonEmpty || field.preTags.nonEmpty) {
            if (field.postTags.isEmpty)
              builder.field("post_tags", util.Arrays.asList("</em>"))
            else
              builder.field("post_tags", field.postTags.asJava)

            if (field.preTags.isEmpty)
              builder.field("pre_tags", util.Arrays.asList("<em>"))
            else
              builder.field("pre_tags", field.preTags.asJava)
          }
          field.requireFieldMatch.foreach(builder.field("require_field_match", _))
          builder.endObject()
        }
        builder.endObject()
      }
      builder.endObject()
    }

    if (request.suggs.nonEmpty) {
      builder.startObject("suggest")
      request.suggs.foreach {
        case term: TermSuggestionDefinition =>
          builder.startObject(term.name)
          term.text.foreach(builder.field("text", _))
          builder.startObject("term")
          builder.field("field", term.fieldname)
          term.analyzer.foreach(builder.field("analyzer", _))
          term.lowercaseTerms.foreach(builder.field("lowercase_terms", _))
          term.maxEdits.foreach(builder.field("max_edits", _))
          term.minWordLength.foreach(builder.field("min_word_length", _))
          term.maxInspections.foreach(builder.field("max_inspections", _))
          term.minDocFreq.foreach(builder.field("min_doc_freq", _))
          term.maxTermFreq.foreach(builder.field("max_term_freq", _))
          term.prefixLength.foreach(builder.field("prefix_length", _))
          term.size.foreach(builder.field("size", _))
          term.shardSize.foreach(builder.field("shard_size", _))
          term.sort.map(_.name().toLowerCase).foreach(builder.field("sort", _))
          term.stringDistance.map(_.name.toLowerCase).foreach(builder.field("string_distance", _))
          term.suggestMode.map(_.name().toLowerCase).foreach(builder.field("suggest_mode", _))
          builder.endObject()
          builder.endObject()
        case completion: CompletionSuggestionDefinition =>
          builder.startObject(completion.name)
          completion.text.foreach(builder.field("text", _))
          completion.prefix.foreach(builder.field("prefix", _))
          completion.regex.foreach(builder.field("regex", _))
          completion.analyzer.foreach(builder.field("analyzer", _))
          builder.startObject("completion")
          builder.field("field", completion.fieldname)
          completion.size.foreach(builder.field("size", _))
          completion.fuzziness.foreach { fuzziness =>
            builder.startObject("fuzzy")
            fuzziness.toXContent(builder, ToXContent.EMPTY_PARAMS)
            completion.transpositions.foreach(builder.field("transpositions", _))
            completion.fuzzyMinLength.foreach(builder.field("min_length", _))
            completion.fuzzyPrefixLength.foreach(builder.field("prefix_length", _))
            completion.unicodeAware.foreach(builder.field("unicode_aware", _))
            builder.endObject()
          }
          completion.regexOptions.foreach { regexOptions =>
            regexOptions.toXContent(builder, ToXContent.EMPTY_PARAMS)
          }
          builder.endObject()
          builder.endObject()
      }
      builder.endObject()
    }

    if (request.storedFields.nonEmpty) {
      builder.field("stored_fields", request.storedFields.asJava)
    }

    if (request.indexBoosts.nonEmpty) {
      builder.startArray("indices_boost")
      request.indexBoosts.foreach { case (name, double) =>
        builder.startObject()
        builder.field(name, double)
        builder.endObject()
      }
      builder.endArray()
    }

    // source filtering
    request.fetchContext foreach { context =>
      if (context.fetchSource) {
        if (context.includes.nonEmpty || context.excludes.nonEmpty) {
          builder.startObject("_source")
          builder.field("includes", context.includes.toList.asJava)
          builder.field("excludes", context.excludes.toList.asJava)
          builder.endObject()
        }
      } else {
        builder.field("_source", false)
      }
    }

    // aggregations
    if (request.aggs.nonEmpty) {
      builder.startObject("aggregations")
      request.aggs.foreach { agg =>
        builder.rawField(agg.name, AggregationBuilderFn(agg).bytes, XContentType.JSON)
      }
      builder.endObject()
    }

    builder.endObject()
    builder
  }
}
