package com.mk.hello.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.serialization.CompressedJsonable;
import com.lightbend.lagom.serialization.Jsonable;
import com.mk.hello.api.BlogService;
import com.mk.hello.api.PostContent;
import com.mk.hello.api.UpdateContent;

import java.util.Optional;

import javax.annotation.concurrent.Immutable;

import akka.Done;
import com.mk.hello.api.UpdateContent;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * Commands for manipulating blog post entities.
 *
 * @author Matt Sicker
 * @see BlogEntity
 */
public interface BlogCommand extends Jsonable {

  /**
   * @see BlogService#getPost(String)
   */
  enum GetPost implements BlogCommand, PersistentEntity.ReplyType<Optional<PostContent>> {
    INSTANCE
  }

  /**
   * @see BlogService#addPost()
   */
  @Immutable
  @JsonDeserialize
  @Value
  @AllArgsConstructor(onConstructor = @__(@JsonCreator))
  final class AddPost implements BlogCommand, CompressedJsonable, PersistentEntity.ReplyType<String> {
    @NonNull
    PostContent content;
  }

  /**
   * @see BlogService#updatePost(String)
   */
  @Immutable
  @JsonDeserialize
  @Value
  @AllArgsConstructor(onConstructor = @__(@JsonCreator))
  final class UpdatePost implements BlogCommand, CompressedJsonable, PersistentEntity.ReplyType<Done> {
    @NonNull
    UpdateContent content;
  }

  /**
   * @see BlogService#deletePost(String)
   */
  enum DeletePost implements BlogCommand, PersistentEntity.ReplyType<Done> {
    INSTANCE
  }
}
