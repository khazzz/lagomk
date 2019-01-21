package com.mk.hello.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.annotation.concurrent.Immutable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;

/**
 * Model for holding a blog post.
 *
 * @author Matt Sicker
 */
@Immutable
@JsonDeserialize
@Value
@Builder
@Wither
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public final class PostSummary {

    @NonNull
    String id;
    @NonNull
    String title;
}
