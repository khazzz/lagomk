package com.mk.hello.impl;

import akka.Done;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber.Probe;
import akka.stream.testkit.javadsl.TestSink;
import com.lightbend.lagom.javadsl.api.Service;
import com.mk.hello.api.BlogService;
import com.mk.hello.api.CrudService;
import com.mk.hello.api.PostContent;
import com.mk.hello.api.PostSummary;
import org.junit.Test;

import java.util.Optional;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.withServer;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;

public class CrudServiceTest {

  @Test
  public void testPost() throws Exception {
    withServer(defaultSetup().withCassandra(), server -> {
      CrudService service = server.client(CrudService.class);

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
      PostContent[] postContents = new PostContent[15];
      PostSummary[] postSummaries = new PostSummary[15];
      for(int i=0; i<15; i++) {
        postContents[i] = new PostContent("title"+i, "body"+i, "author"+i);
        String id = service.addPost().invoke(postContents[i]).toCompletableFuture().get(5, SECONDS);
        assertFalse(id.isEmpty());

        postSummaries[i] = new PostSummary(id, postContents[i].getTitle());
      }

      // test live post streaming
      Source<PostSummary, ?> livePosts = service.getLivePosts().invoke().toCompletableFuture().get(5, SECONDS);
      Probe<PostSummary> probe = livePosts.runWith(TestSink.probe(server.system()),
              server.materializer());
      probe.request(10);

      for(int i=0; i<10; i++) {
        PostSummary postSummary = probe.expectNext();

        assertTrue(!postSummary.getId().isEmpty());
        assertTrue(!postSummary.getTitle().isEmpty());
      }

      probe.cancel();

      // test live post streaming by author
      Source<PostSummary, ?> livePostsByAuthor = service.getLivePostsByAuthor().invoke("author10").toCompletableFuture().get(5, SECONDS);
      Probe<PostSummary> probe2 = livePostsByAuthor.runWith(TestSink.probe(server.system()),
              server.materializer());
      probe2.request(10);

      PostSummary postSummary = probe2.expectNext();

      assertTrue(!postSummary.getId().isEmpty());
      assertEquals("title10", postSummary.getTitle());

      probe2.cancel();


    });
  }

}
