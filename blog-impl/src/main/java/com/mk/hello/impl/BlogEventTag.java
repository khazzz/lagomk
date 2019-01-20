package com.mk.hello.impl;

import com.lightbend.lagom.javadsl.persistence.AggregateEventShards;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;

public class BlogEventTag {

    /**
     * Tags are used for getting and publishing streams of events. Each event
     * will have this tag, and in this case, we are partitioning the tags into
     * 4 shards, which means we can have 4 concurrent processors/publishers of
     * events.
     */
    public static final AggregateEventShards<BlogEvent> TAG = AggregateEventTag.sharded(BlogEvent.class, 4);
}
