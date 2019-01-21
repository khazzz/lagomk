package com.mk.hello.impl;

import akka.Done;
import com.mk.hello.api.BlogService;
import com.mk.hello.api.PostContent;
import com.mk.hello.api.PostSummary;
import org.junit.Test;
import org.pcollections.PSequence;

import java.util.Optional;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.withServer;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;

public class BlogServiceTest {

  @Test
  public void testPost() throws Exception {
    withServer(defaultSetup().withCassandra(), server -> {
      BlogService service = server.client(BlogService.class);

      // test add post
      String id1 = service.addPost().invoke(new PostContent("title", "body", "author")).toCompletableFuture().get(5, SECONDS);
      assertFalse(id1.isEmpty());

      // test get post
      Optional<PostContent> contentAfterAdd = service.getPost(id1).invoke().toCompletableFuture().get(5, SECONDS);
      assertTrue(contentAfterAdd.isPresent());
      assertEquals("title", contentAfterAdd.get().getTitle());
      assertEquals("body", contentAfterAdd.get().getBody());
      assertEquals("author", contentAfterAdd.get().getAuthor());

      // test update post
      Done doneUpdate = service.updatePost(id1).invoke(new PostContent("title_updated", "body_updated", "author_updated")).toCompletableFuture().get(5, SECONDS);
      assertTrue(doneUpdate.equals(Done.done()));

      // test get post with updated values
      Optional<PostContent> contentAfterUpdate = service.getPost(id1).invoke().toCompletableFuture().get(5, SECONDS);
      assertTrue(contentAfterUpdate.isPresent());
      assertEquals("title_updated", contentAfterUpdate.get().getTitle());
      assertEquals("body_updated", contentAfterUpdate.get().getBody());
      assertEquals("author_updated", contentAfterUpdate.get().getAuthor());

      // test delete post
      Done doneDelete = service.deletePost(id1).invoke().toCompletableFuture().get(5, SECONDS);
      assertTrue(doneDelete.equals(Done.done()));

      // test get post with after delete
      Optional<PostContent> contentAfterDelete = service.getPost(id1).invoke().toCompletableFuture().get(5, SECONDS);
      assertTrue(!contentAfterDelete.isPresent());
      assertEquals(contentAfterDelete, Optional.empty());

      // test add post(s)
      for(int i=0; i<15; i++) {
        String id = service.addPost().invoke(new PostContent("title"+i, "body"+i, "author"+i)).toCompletableFuture().get(5, SECONDS);
        assertFalse(id.isEmpty());
      }

      //PSequence<PostSummary> allPosts = service.getAllPosts(0, 10).invoke().toCompletableFuture().get(5, SECONDS);
      //assertTrue(allPosts.size() == 10);

    });
  }

}
