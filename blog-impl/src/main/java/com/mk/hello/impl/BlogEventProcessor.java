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
import org.pcollections.TreePVector;

import javax.inject.Inject;

import static com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide.completedStatement;

public class BlogEventProcessor extends ReadSideProcessor<BlogEvent> {

    private final CassandraSession session;
    private final CassandraReadSide readSide;

    private PreparedStatement writePreparedStatement = null; // initialized in prepare
    private PreparedStatement updatePreparedStatement = null; // initialized in prepare

    @Inject
    public BlogEventProcessor(CassandraSession session, CassandraReadSide readSide) {
        this.session = session;
        this.readSide = readSide;
    }

    private void setWritePreparedStatement(PreparedStatement writePreparedStatement) {
        this.writePreparedStatement = writePreparedStatement;
    }
    private void setUpdatePreparedStatement(PreparedStatement updatePreparedStatement) {
        this.updatePreparedStatement = updatePreparedStatement;
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
//                .setPrepare((ignored) -> prepareUpdateBlog())
//                .setEventHandler(BlogEvent.PostUpdated.class, this::processPostUpdated)
                .build();
    }

    private CompletionStage<Done> prepareCreateTables() {
        // @formatter:off
        return session.executeCreateTable(
                "CREATE TABLE IF NOT EXISTS postcontent ("
                        + "id text, title text, body text, author text, "
                        + "PRIMARY KEY (id))");
        // @formatter:on
    }

    private CompletionStage<Done> prepareWriteBlog() {
        return session.prepare("INSERT INTO postcontent (id, title, body, author) VALUES (?, ?, ?, ?)").thenApply(ps -> {
            setWritePreparedStatement(ps);
            return Done.getInstance();
        });
    }

    private CompletionStage<Done> prepareUpdateBlog() {
        return session.prepare("UPDATE postcontent set title = ?, body = ?, author = ? where id = ? IF EXISTS").thenApply(ps -> {
            setUpdatePreparedStatement(ps);
            return Done.getInstance();
        });
    }

    private CompletionStage<List<BoundStatement>> processPostAdded(BlogEvent.PostAdded event) {
        BoundStatement bindWritePreparedStatement = writePreparedStatement.bind();
        bindWritePreparedStatement.setString("id", event.getId());
        bindWritePreparedStatement.setString("title", event.getContent().getTitle());
        bindWritePreparedStatement.setString("body", event.getContent().getBody());
        bindWritePreparedStatement.setString("author", event.getContent().getAuthor());
        return completedStatement(bindWritePreparedStatement);
    }

    private CompletionStage<List<BoundStatement>> processPostUpdated(BlogEvent.PostUpdated event) {
        BoundStatement bindUpdatePreparedStatement = updatePreparedStatement.bind();
        bindUpdatePreparedStatement.setString("title", event.getContent().getTitle());
        bindUpdatePreparedStatement.setString("body", event.getContent().getBody());
        bindUpdatePreparedStatement.setString("author", event.getContent().getAuthor());
        bindUpdatePreparedStatement.setString("id", event.getId());
        return completedStatement(bindUpdatePreparedStatement);
    }

}