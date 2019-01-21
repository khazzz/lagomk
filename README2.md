Lagom framework for java experiments.

I adopted a blog project from https://github.com/jvz/lagom-example
and added missing functionality. Now it supports write-side CQRS with
add, update, delete and findOne commands/events.

I also added a read-side support where events from the write-side are converted
to the read views (tables for select queires).

I also created a crud-api and crud-impl projects that do not follow CQRS model.
It is a different implementation of a blog project. I am just using prepared statements
directly to do all of the crud operations.

Conclusion: When atempting to write microservices with a Lagom framework for java, one
has a choice to follow a true CQRS event sourcing model that is implemented in a blog-api/blog-impl.
Or one can use it like a hack by performing crud operations directly. In both cases you get none blocking
calls.
