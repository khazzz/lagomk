package com.mk.hello.api;

import akka.stream.javadsl.Source;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.transport.Method;

import java.util.Optional;

import akka.Done;
import akka.NotUsed;
import org.pcollections.PSequence;

import static com.lightbend.lagom.javadsl.api.Service.*;

/**
 * Exposes the crud microservice API. This API is adapted from the Lagom documentation.
 *
 * @author Matt Sicker
 */
public interface CrudService extends Service {

  /**
   * Gets a crud post for the given ID. Example:
   * curl http://localhost:9000/api/crud/12345678-1234-1234-1234-1234567890ab
   *
   * @param id id of the crud post to get
   */
  ServiceCall<NotUsed, Optional<PostContent>> getPost(String id);

  /**
   * Creates a new crud post and returns the ID of the newly created post. Example:
   * curl -H 'content-type: application/json' -X POST
   * -d '{"title": "Some Title", "body": "Some body", "author": "Some Guy"}'
   * http://localhost:9000/api/crud/
   */
  ServiceCall<PostContent, String> addPost();

  /**
   * Submits a crud post for the given ID. Example:
   * curl -H 'content-type: application/json' -X PUT
   * -d '{"title": "Some Title", "body": "Some body", "author": "Some Guy"}'
   * http://localhost:9000/api/crud/12345678-1234-1234-1234-1234567890ab
   *
   * @param id id of crud post to updatePost
   */
  ServiceCall<PostContent, Done> updatePost(String id);

  /**
   * Delete a crud post for the given ID. Example:
   * curl -X DELETE http://localhost:9000/api/crud/12345678-1234-1234-1234-1234567890ab
   *
   * @param id id of crud post to deletePost
   */
  ServiceCall<NotUsed, Done> deletePost(String id);

  /**
   * Gets all crud posts. Example:
   * curl http://localhost:9000/api/crud/pageNo/:pageNo/pageSize/:pageSize
   *
   * @param pageNo - limit to this pageNo
   * @param pageSize - limit to this pageSize
   */
  ServiceCall<NotUsed, PSequence<PostSummary>> getAllPosts(Integer pageNo, Integer pageSize);

  /**
   * Gets all crud posts. Example:
   * curl http://localhost:9000/api/crud/live
   *
   */
  //ServiceCall<NotUsed, Source<PostSummary, ?>> getLivePosts();

  /**
   * Gets all crud posts by author. Example:
   * curl http://localhost:9000/api/crud/author/:author/pageNo/:pageNo/pageSize/:pageSize
   *
   * @param author - search by author
   * @param pageNo - limit to this pageNo
   * @param pageSize - limit to this pageSize
   */
  ServiceCall<NotUsed, PSequence<PostSummary>> getPostsByAuthor(String author, Integer pageNo, Integer pageSize);

  @Override
  default Descriptor descriptor() {
    return named("crud").withCalls(
            restCall(Method.GET, "/api/crud/:id", this::getPost),
            restCall(Method.POST, "/api/crud/", this::addPost),
            restCall(Method.PUT, "/api/crud/:id", this::updatePost),
            restCall(Method.DELETE, "/api/crud/:id", this::deletePost),
            restCall(Method.GET, "/api/crud/pageNo/:pageNo/pageSize/:pageSize", this::getAllPosts),
            //namedCall("/api/crud/live/", this::getLivePosts),
            restCall(Method.GET, "/api/crud/author/:author/pageNo/:pageNo/pageSize/:pageSize", this::getPostsByAuthor)
    ).withAutoAcl(true);
  }
}