# What ?

A full-stack webapp with responsive, interactive pages.
It has efficient feedback loops and hot reloading.
Almost like Next.js, but less complexity.

- KTor as a web server — https://ktor.io
- Kotlin typesafe HTML DSL - https://kotlinlang.org/docs/typesafe-html-dsl.html
- HTMX for front end interactivity — https://htmx.org
- Pico CSS for responsive styling and semantic defaults — https://picocss.com

You can try the "application" here: https://kotlin-htmx.fly.dev/ .
On the first page you can find links to the different demos.

There's a lengthy blog post about KTor SSE and HTMX (one of the demos) here:
https://blog.f12.no/wp/2024/11/11/htmx-sse-easy-updates-of-html-state-with-no-javascript/ .

# Other stuff

- HTMX examples to do all kinds of different back-ends (C#, Ruby, Spring Boot,
  Ocaml +++) - https://htmx.org/server-examples/
- Quite a similar Kotlin example (referenced from HTMX itself) — https://github.com/Rattlyy/htmx-ktor/tree/master

# Why?

Separate builds for the back end and front end are manageable
but give a worse feedback loop than actually having it all integrated.
It is one of the benefits of frameworks like Next.js.
But those frameworks also come with a lot of complexities in the build pipelines, and in the runtime.

I believe this "stack" gives a simpler environment that is easier to monitor, debug and maintain over time.

It is a big leap from the SPA pages we are used to, but it is worth it. Don't dismiss it before actually trying.

Sometimes you should not use it, but I do think starting with HTMX and moving on when needed is a much better approach
than the other way around. HTMX has a really fair and balanced article for when to use it
here: https://htmx.org/essays/when-to-use-hypermedia/

# Discuss

Let me know what you think!

You can find me here:

- https://twitter.com/anderssv
- https://www.linkedin.com/in/anderssv/
- https://blog.f12.no/wp/
- https://www.mikill.no

# Running

## Local environment

Set environment variables directly or in a .env.local file.

See the possible variables in ```.env.default```, you can copy it to .env.local to do local modifications. Or you can
set the variables in env.

## Starting

Run the ```fun main()``` in Application.kt in your favourite IDE, or run:

    $ ./gradlew run

It is then available on [http://localhost:8080](http://localhost:8080).

# Other notes

I used KTor generator to get started.

We load _everything_ via ```<script />``` tags to avoid having any kind of build steps for the front end.
These dependencies should probably be checked in to the repository for production use.

## Back end code structure

One of the downsides of this is that there are few "best practices" and recommended structures. But that is
also the benefit. You can use any library or structure you like. [Thymeleaf](https://www.thymeleaf.org/) is commonly
used, but there are others too.

It is still important to have some kind of defaults for route, controller and pages. YMMV.

## Bigger scale and functionality?

This is a small example with simple functionality.
Performance vice there is no reason this won't scale,
but for some features you will add additional JavaScript libraries.
Something like [AlpineJS](https://alpinejs.dev/) seems to go well together with HTMX.
The important part is that you start with something, and add as needed.
You do not have to choose one-big-framework to rule-them-all.

People are doing it in production.
See this article and presentations for a real-world migration from React to HTMX:
https://htmx.org/essays/a-real-world-react-to-htmx-port/
