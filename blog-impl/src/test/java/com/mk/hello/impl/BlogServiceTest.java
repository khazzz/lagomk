package com.mk.hello.impl;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber.Probe;
import akka.stream.testkit.javadsl.TestSink;

import com.mk.hello.api.BlogService;
import com.mk.hello.api.PostContent;
import com.mk.hello.api.PostSummary;
import com.mk.hello.api.UpdateContent;
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
      Done doneUpdate = service.updatePost(id1).invoke(new UpdateContent("title_updated", "body_updated")).toCompletableFuture().get(5, SECONDS);
      assertTrue(doneUpdate.equals(Done.done()));

      // test get post with updated values
      Optional<PostContent> contentAfterUpdate = service.getPost(id1).invoke().toCompletableFuture().get(5, SECONDS);
      assertTrue(contentAfterUpdate.isPresent());
      assertEquals("title_updated", contentAfterUpdate.get().getTitle());
      assertEquals("body_updated", contentAfterUpdate.get().getBody());
      assertEquals("author", contentAfterUpdate.get().getAuthor());

      // test delete post
      Done doneDelete = service.deletePost(id1).invoke().toCompletableFuture().get(5, SECONDS);
      assertTrue(doneDelete.equals(Done.done()));

      // test get post with after delete
      Optional<PostContent> contentAfterDelete = service.getPost(id1).invoke().toCompletableFuture().get(5, SECONDS);
      assertTrue(!contentAfterDelete.isPresent());
      assertEquals(contentAfterDelete, Optional.empty());

      // test add post(s)
      PostContent[] postContents = new PostContent[10];
      PostSummary[] postSummaries = new PostSummary[10];
      for(int i=0; i<10; i++) {
        postContents[i] = new PostContent("title"+i, "body"+i, "author");
        String id = service.addPost().invoke(postContents[i]).toCompletableFuture().get(5, SECONDS);
        assertFalse(id.isEmpty());

        postSummaries[i] = new PostSummary(id, postContents[i].getTitle());
      }

      Source<PostSummary, ?> livePosts = service.getLivePostsByAuthor().invoke("author").toCompletableFuture().get(5, SECONDS);
      Probe<PostSummary> probe = livePosts.runWith(TestSink.probe(server.system()),
              server.materializer());
      probe.request(10);

      /*for(int i=0; i<10; i++) {
        PostSummary postSummary = probe.expectNext();

        assertTrue(!postSummary.getId().isEmpty());
        assertTrue(!postSummary.getTitle().isEmpty());
      }*/

      probe.cancel();

      //PSequence<PostSummary> allPosts = service.getAllPosts(0, 10).invoke().toCompletableFuture().get(5, SECONDS);
      //assertTrue(allPosts.size() == 10);

    });
  }

}
