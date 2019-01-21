package com.mk.hello.impl;

import akka.stream.javadsl.Source;

import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.lightbend.lagom.javadsl.persistence.ReadSide;
import com.mk.hello.api.BlogService;
import com.mk.hello.api.PostContent;
import com.mk.hello.api.PostSummary;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;
import com.datastax.driver.core.Row;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import javax.inject.Inject;

import akka.Done;
import akka.NotUsed;
import org.pcollections.PSequence;
import org.pcollections.TreePVector;

/**
 * Service implementation for the blog microservice. This service is essentially a wrapper for the
 * persistence entity API.
 *
 * @author Matt Sicker
 */
public class BlogServiceImpl implements BlogService {

  private final PersistentEntityRegistry registry;
  private final CassandraSession db;

  @Inject
  public BlogServiceImpl(final PersistentEntityRegistry registry, ReadSide readSide,
                         CassandraSession db) {
    this.registry = registry;
    this.db = db;

    registry.register(BlogEntity.class);
    readSide.register(BlogEventProcessor.class);
  }

  @Override
  public ServiceCall<NotUsed, Optional<PostContent>> getPost(final String id) {
    return request -> registry.refFor(BlogEntity.class, id)
            .ask(BlogCommand.GetPost.INSTANCE);
  }

  @Override
  public ServiceCall<PostContent, String> addPost() {
    return content -> registry.refFor(BlogEntity.class, UUID.randomUUID().toString())
            .ask(new BlogCommand.AddPost(content));
  }

  @Override
  public ServiceCall<PostContent, Done> updatePost(final String id) {
    return content -> registry.refFor(BlogEntity.class, id)
            .ask(new BlogCommand.UpdatePost(content));
  }

  @Override
  public ServiceCall<NotUsed, Done> deletePost(final String id) {
    return request -> registry.refFor(BlogEntity.class, id)
            .ask(BlogCommand.DeletePost.INSTANCE);
  }

  @Override
  public ServiceCall<NotUsed, PSequence<PostSummary>> getAllPosts() {
    return req -> {
      CompletionStage<PSequence<PostSummary>> result = db.selectAll("SELECT * FROM postcontent")
              .thenApply(rows -> {
                List<PostSummary> posts = rows.stream().map(this::mapPostSummary).collect(Collectors.toList());
                return TreePVector.from(posts);
              });
      return result;
    };
  }

  @Override
  public ServiceCall<NotUsed, Source<PostSummary, ?>> getLivePosts() {
    return req -> {

      Source<PostSummary, ?> result = db.select("SELECT * FROM postcontent")
              .map(this::mapPostSummary);
      return CompletableFuture.completedFuture(result);
    };
  }

  @Override
  public ServiceCall<NotUsed, PSequence<PostSummary>> getPostsByAuthor(final String author) {
    return req -> {
      CompletionStage<PSequence<PostSummary>> result = db.selectAll("SELECT * FROM postcontent")
              .thenApply(rows -> {
                List<PostSummary> posts = rows.stream()
                        .filter(row -> row.getString("author").equalsIgnoreCase(author))
                        .map(this::mapPostSummary).collect(Collectors.toList());
                return TreePVector.from(posts);
              });
      return result;
    };
  }

  private PostSummary mapPostSummary(Row row) {
    return new PostSummary(
            row.getString("id"),
            row.getString("title")
    );
  }

}