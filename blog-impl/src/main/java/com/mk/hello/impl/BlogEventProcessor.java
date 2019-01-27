package com.mk.hello.impl;

import java.util.List;
import java.util.concurrent.CompletionStage;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.ReadSideProcessor;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;

import akka.Done;
import org.pcollections.PSequence;

import javax.inject.Inject;

import static com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide.completedStatement;

public class BlogEventProcessor extends ReadSideProcessor<BlogEvent> {

    private final CassandraSession session;
    private final CassandraReadSide readSide;

    private PreparedStatement writePreparedStatement = null; // initialized in prepare
    private PreparedStatement updatePreparedStatement = null; // initialized in prepare
    private PreparedStatement deletePreparedStatement = null; // initialized in prepare


    @Inject
    public BlogEventProcessor(CassandraSession session, CassandraReadSide readSide) {
        this.session = session;
        this.readSide = readSide;
    }

    @Override
    public PSequence<AggregateEventTag<BlogEvent>> aggregateTags() {
        return BlogEventTag.TAG.allTags();
    }

    @Override
    public ReadSideHandler<BlogEvent> buildHandler() {
        return readSide.<BlogEvent>builder("blog_offset")
                .setGlobalPrepare(this::prepareCreateTables)
                .setPrepare((ignored) -> prepareWriteBlog())
                .setEventHandler(BlogEvent.PostAdded.class, this::processPostAdded)
                .setEventHandler(BlogEvent.PostUpdated.class, this::processPostUpdated)
                .setEventHandler(BlogEvent.PostDeleted.class, this::processPostDeleted)
                .build();
    }

    private CompletionStage<Done> prepareCreateTables() {
        // @formatter:off
        return session.executeCreateTable(
                "CREATE TABLE IF NOT EXISTS postcontent ("
                        + "id text, timestamp bigint, title text, body text, author text, "
                        + "PRIMARY KEY (id))");
        // @formatter:on
    }

    private CompletionStage<Done> prepareWriteBlog() {

        // prepare insert statement
        return session.prepare("INSERT INTO postcontent (id, timestamp, title, body, author) VALUES (?, ?, ?, ?, ?)").thenApply(ps -> {
            writePreparedStatement = ps;

            // prepare update statement
            prepareUpdateBlog();

            // prepare delete statement
            prepareDeleteBlog();

            return Done.getInstance();
        });
    }

    private CompletionStage<Done> prepareUpdateBlog() {
        return session.prepare("UPDATE postcontent set title = ?, body = ?, author = ?, timestamp = ? where id = ?").thenApply(ps -> {
            updatePreparedStatement = ps;
            return Done.getInstance();
        });
    }

    private CompletionStage<Done> prepareDeleteBlog() {
        return session.prepare("DELETE FROM postcontent WHERE id = ?").thenApply(ps -> {
            deletePreparedStatement = ps;
            return Done.getInstance();
        });
    }

    private CompletionStage<List<BoundStatement>> processPostAdded(BlogEvent.PostAdded event) {

        return completedStatement(
                writePreparedStatement.bind(
                        event.getId(),
                        event.getTimestamp().toEpochMilli(),
                        event.getContent().getTitle(),
                        event.getContent().getBody(),
                        event.getContent().getAuthor()
                )
        );
    }

    private CompletionStage<List<BoundStatement>> processPostUpdated(BlogEvent.PostUpdated event) {

        return completedStatement(
                updatePreparedStatement.bind(
                        event.getContent().getTitle(),
                        event.getContent().getBody(),
                        event.getContent().getAuthor(),
                        event.getTimestamp().toEpochMilli(),
                        event.getId()
                )
        );
    }

    private CompletionStage<List<BoundStatement>> processPostDeleted(BlogEvent.PostDeleted event) {

        return completedStatement(
                deletePreparedStatement.bind(
                        event.getId()
                )
        );
    }

}
