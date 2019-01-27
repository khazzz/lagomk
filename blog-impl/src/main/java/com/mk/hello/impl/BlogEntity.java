package com.mk.hello.impl;

import com.lightbend.lagom.javadsl.persistence.PersistentEntity;

import java.util.Optional;
import java.time.Instant;

import akka.Done;
import com.mk.hello.api.PostContent;

/**
 * Entity and behaviors for blog posts.
 *
 * @author Matt Sicker
 */
@SuppressWarnings("unchecked")
public class BlogEntity extends PersistentEntity<BlogCommand, BlogEvent, BlogState> {

  @Override
  public Behavior initialBehavior(final Optional<BlogState> snapshotState) {
    final BehaviorBuilder b = newBehaviorBuilder(snapshotState.orElse(BlogState.EMPTY));
    addBehaviorForGetPost(b);
    addBehaviorForAddPost(b);
    addBehaviorForUpdatePost(b);
    addBehaviorForDeletePost(b);
    return b.build();
  }

  private void addBehaviorForGetPost(final BehaviorBuilder b) {
    b.setReadOnlyCommandHandler(BlogCommand.GetPost.class,
            (cmd, ctx) -> ctx.reply(state().getContent()));
  }

  private void addBehaviorForAddPost(final BehaviorBuilder b) {
    b.setCommandHandler(BlogCommand.AddPost.class,
            (cmd, ctx) -> ctx.thenPersist(
                    new BlogEvent.PostAdded(entityId(), Instant.now(), cmd.getContent()),
                    evt -> ctx.reply(entityId())
            )
    );
    b.setEventHandler(BlogEvent.PostAdded.class, evt -> new BlogState(Optional.of(evt.getContent()), Optional.of(evt.getTimestamp())));
  }

  private void addBehaviorForUpdatePost(final BehaviorBuilder b) {
    b.setCommandHandler(BlogCommand.UpdatePost.class,
            (cmd, ctx) -> ctx.thenPersist(
                    new BlogEvent.PostUpdated(state().getTimestamp().get(), new PostContent(
                            cmd.getContent().getTitle(), cmd.getContent().getBody(), state().getContent().map(c->c.getAuthor()).orElseThrow(RuntimeException::new))),
                    evt -> ctx.reply(Done.getInstance())
            )
    );
    b.setEventHandler(BlogEvent.PostUpdated.class, evt -> new BlogState(Optional.of(evt.getContent()), Optional.of(evt.getTimestamp())));
  }

  private void addBehaviorForDeletePost(final BehaviorBuilder b) {
    b.setCommandHandler(BlogCommand.DeletePost.class,
            (cmd, ctx) -> ctx.thenPersist(
                    new BlogEvent.PostDeleted(state().getContent().map(c->c.getAuthor()).orElseThrow(RuntimeException::new), state().getTimestamp().get()),
                    evt -> ctx.reply(Done.getInstance())
            )
    );
    // reset the snapshot state to empty
    b.setEventHandler(BlogEvent.PostDeleted.class, evt -> BlogState.EMPTY);
  }
}