package com.mk.hello.impl;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import com.mk.hello.api.BlogService;

/**
 * The module that binds the BlogService so that it can be served.
 */
public class BlogModule extends AbstractModule implements ServiceGuiceSupport {
  @Override
  protected void configure() {
    bindService(BlogService.class, BlogServiceImpl.class);
  }
}
