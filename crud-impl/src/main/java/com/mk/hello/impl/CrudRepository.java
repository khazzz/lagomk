package com.mk.hello.impl;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.datastax.driver.core.Row;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;
import com.mk.hello.api.PostContent;
import com.mk.hello.api.PostSummary;
import org.pcollections.PSequence;
import org.pcollections.TreePVector;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Singleton
public class CrudRepository {
  private final CassandraSession uninitialisedSession;

  // Will return the session when the Cassandra tables have been successfully created
  private volatile CompletableFuture<CassandraSession> initialisedSession;

  @Inject
  public CrudRepository(CassandraSession uninitialisedSession) {
    this.uninitialisedSession = uninitialisedSession;
    // Eagerly create the session
    session();
  }

  private CompletionStage<CassandraSession> session() {
    // If there's no initialised session, or if the initialised session future completed
    // with an exception, then reinitialise the session and attempt to create the tables
    if (initialisedSession == null || initialisedSession.isCompletedExceptionally()) {
      initialisedSession = uninitialisedSession.executeCreateTable(
              "CREATE TABLE IF NOT EXISTS post_content ("
              + "id text, title text, body text, author text, "
              + "PRIMARY KEY (id))"
      ).thenApply(done -> uninitialisedSession).toCompletableFuture();
    }
    return initialisedSession;
  }

  public CompletionStage<String> addPost(PostContent content) {
    String id = UUID.randomUUID().toString();

    return session().thenCompose(session ->
        session.executeWrite("INSERT INTO post_content (id, title, body, author) VALUES (?, ?, ?, ?)",
            id, content.getTitle(), content.getBody(), content.getAuthor()))
                .thenApply(ignored -> id);
  }

  public CompletionStage<Done> updatePost(String id, PostContent content) {

    return session().thenCompose(session ->
            session.executeWrite("UPDATE post_content set title = ?, body = ?, author = ? WHERE id = ? IF EXISTS",
                    content.getTitle(), content.getBody(), content.getAuthor(), id));
  }

  public CompletionStage<Done> deletePost(String id) {

    return session().thenCompose(session ->
            session.executeWrite("DELETE FROM post_content WHERE id = ? IF EXISTS",
                    id));
  }

  public CompletionStage<Optional<PostContent>> getPost(String id) {

    return session().thenCompose(session ->
            session.selectOne("SELECT * FROM post_content where id = ?", id)
    ).thenApply(maybeRow -> maybeRow.map(this::mapPostContent));
  }

  /**
   * Note: It is a bad idea to use select * without a limit cuz session.selectAll returns
   * a List<Row> and you will run out of memory. Instead use session.select if you have no
   * limit and return a Source<Row> and let the client control the back pressure. The client
   * will use a web socket instead of a REST call.
   *
   * @param pageNo
   * @param pageSize
   * @return
   */
  public CompletionStage<PSequence<PostSummary>> getAllPosts(Integer pageNo, Integer pageSize) {

       return session().thenCompose(session ->
              session.selectAll("SELECT * FROM post_content")
       ).thenApply(rows -> {
                List<PostSummary> posts = rows.stream()
                        .skip(pageNo*pageSize)
                        .limit(pageSize)
                        .map(this::mapPostSummary).collect(Collectors.toList());
                return TreePVector.from(posts);
              });
  }

  public CompletionStage<Source<PostSummary, ?>> getAllPosts() {

    return session().thenCompose(session ->
            CompletableFuture.completedFuture(
                    session.select("SELECT * FROM post_content")
                            .map(this::mapPostSummary)));
  }

  public CompletionStage<Source<PostSummary, ?>> getPostsByAuthor(String author) {

    return session().thenCompose(session ->
            CompletableFuture.completedFuture(
                    session.select("SELECT * FROM post_content")
                            .filter(row -> row.getString("author").equalsIgnoreCase(author))
                            .map(this::mapPostSummary)));
  }

  public CompletionStage<PSequence<PostSummary>> getPostsByAuthor(String author, Integer pageNo, Integer pageSize) {

    return session().thenCompose(session ->
            session.selectAll("SELECT * FROM post_content")
    ).thenApply(rows -> {
      List<PostSummary> posts = rows.stream()
              .filter(row -> row.getString("author").equalsIgnoreCase(author))
              .skip(pageNo*pageSize)
              .limit(pageSize)
              .map(this::mapPostSummary).collect(Collectors.toList());
      return TreePVector.from(posts);
    });
  }

  private PostSummary mapPostSummary(Row row) {
    return new PostSummary(
            row.getString("id"),
            row.getString("title")
    );
  }

  private PostContent mapPostContent(Row row) {
    return new PostContent(
            row.getString("title"),
            row.getString("body"),
            row.getString("author")
    );
  }
}
