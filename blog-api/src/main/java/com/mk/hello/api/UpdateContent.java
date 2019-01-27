package com.mk.hello.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;

import javax.annotation.concurrent.Immutable;

/**
 * Model for holding a blog post for update only.
 *
 * @author Matt Sicker
 */
@Immutable
@JsonDeserialize
@Value
@Builder
@Wither
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public final class UpdateContent {

    @NonNull
    String title;
    @NonNull
    String body;
}