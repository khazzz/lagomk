package com.mk.hello.impl;

import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;

public class BlogEventTag {

    public static final AggregateEventTag<BlogEvent> INSTANCE =
            AggregateEventTag.of(BlogEvent.class);
}
