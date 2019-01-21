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
 * Exposes the blog microservice API. This API is adapted from the Lagom documentation.
 *
 * @author Matt Sicker
 */
public interface BlogService extends Service {

  /**
   * Gets a blog post for the given ID. Example:
   * curl http://localhost:9000/api/blog/12345678-1234-1234-1234-1234567890ab
   *
   * @param id id of the blog post to get
   */
  ServiceCall<NotUsed, Optional<PostContent>> getPost(String id);

  /**
   * Creates a new blog post and returns the ID of the newly created post. Example:
   * curl -H 'content-type: application/json' -X POST
   * -d '{"title": "Some Title", "body": "Some body", "author": "Some Guy"}'
   * http://localhost:9000/api/blog/
   */
  ServiceCall<PostContent, String> addPost();

  /**
   * Submits a blog post for the given ID. Example:
   * curl -H 'content-type: application/json' -X PUT
   * -d '{"title": "Some Title", "body": "Some body", "author": "Some Guy"}'
   * http://localhost:9000/api/blog/12345678-1234-1234-1234-1234567890ab
   *
   * @param id id of blog post to updatePost
   */
  ServiceCall<PostContent, Done> updatePost(String id);

  /**
   * Delete a blog post for the given ID. Example:
   * curl -X DELETE http://localhost:9000/api/blog/12345678-1234-1234-1234-1234567890ab
   *
   * @param id id of blog post to deletePost
   */
  ServiceCall<NotUsed, Done> deletePost(String id);

  /**
   * Gets all blog posts. Example:
   * curl http://localhost:9000/api/blog/pageNo/:pageNo/pageSize/:pageSize
   *
   * @param pageNo - limit to this pageNo
   * @param pageSize - limit to this pageSize
   */
  ServiceCall<NotUsed, PSequence<PostSummary>> getAllPosts(Integer pageNo, Integer pageSize);

  /**
   * Gets all blog posts. Example:
   * curl http://localhost:9000/api/blog/live
   *
   */
  ServiceCall<NotUsed, Source<PostSummary, ?>> getLivePosts();

  /**
   * Gets all blog posts by author. Example:
   * curl http://localhost:9000/api/blog/author/:author/pageNo/:pageNo/pageSize/:pageSize
   *
   * @param author - search by author
   * @param pageNo - limit to this pageNo
   * @param pageSize - limit to this pageSize
   */
  ServiceCall<NotUsed, PSequence<PostSummary>> getPostsByAuthor(String author, Integer pageNo, Integer pageSize);

  @Override
  default Descriptor descriptor() {
    return named("blog").withCalls(
            restCall(Method.GET, "/api/blog/:id", this::getPost),
            restCall(Method.POST, "/api/blog/", this::addPost),
            restCall(Method.PUT, "/api/blog/:id", this::updatePost),
            restCall(Method.DELETE, "/api/blog/:id", this::deletePost),
            restCall(Method.GET, "/api/blog/pageNo/:pageNo/pageSize/:pageSize", this::getAllPosts),
            namedCall("/api/blog/live/", this::getLivePosts),
            restCall(Method.GET, "/api/blog/author/:author/pageNo/:pageNo/pageSize/:pageSize", this::getPostsByAuthor)
    ).withAutoAcl(true);
  }
}