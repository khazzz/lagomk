package com.mk.hello.impl;

import akka.stream.javadsl.Source;

import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.lightbend.lagom.javadsl.persistence.ReadSide;
import com.mk.hello.api.BlogService;
import com.mk.hello.api.CrudService;
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

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Service implementation for the crud microservice. This service is essentially a wrapper for the
 * persistence entity API.
 *
 */
public class CrudServiceImpl implements CrudService {

  private final CrudRepository repository;

  @Inject
  public CrudServiceImpl(CrudRepository repository) {

    this.repository = repository;

  }

  @Override
  public ServiceCall<NotUsed, Optional<PostContent>> getPost(final String id) {
    return content -> repository.getPost(id);
  }

  @Override
  public ServiceCall<PostContent, String> addPost() {
    return content -> repository.addPost(content);
  }

  @Override
  public ServiceCall<PostContent, Done> updatePost(final String id) {
    return content -> repository.updatePost(id, content);
  }

  @Override
  public ServiceCall<NotUsed, Done> deletePost(final String id) {
    return content -> repository.deletePost(id);
  }

  @Override
  public ServiceCall<NotUsed, PSequence<PostSummary>> getAllPosts(Integer pageNo, Integer pageSize) {
    return req -> {

      CompletionStage<PSequence<PostSummary>> result = repository.getAllPosts(pageNo, pageSize);

      return result;
    };
  }

  @Override
  public ServiceCall<NotUsed, PSequence<PostSummary>> getPostsByAuthor(final String author, Integer pageNo, Integer pageSize) {
    return req -> {

      CompletionStage<PSequence<PostSummary>> result = repository.getPostsByAuthor(author, pageNo, pageSize);

      return result;
    };
  }
}