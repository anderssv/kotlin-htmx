# What ?

A full stack webapp with responsive, interactive pages. It has efficient feedback loops and hot reloading. Almost like NextJS, but less complexity.

- KTor as a web server - https://ktor.io
- Kotlin typesafe HTML DSL - https://kotlinlang.org/docs/typesafe-html-dsl.html
- HTMX for front end interactivity - https://htmx.org
- Pico CSS for responsive styling and semantic defaults - https://picocss.com

You can try the "application" here: https://kotlin-htmx.fly.dev/

There are also some other sub pages that I use to demo stuff:
- https://kotlin-htmx.fly.dev/demo/form - Shows off form handling with validatoin
- https://kotlin-htmx.fly.dev/demo/multi - Shows off HTMX but also how to integrate other tehcnologies like React and Lit without a full separate project and build.

# Other stuff

- HTMX references to do all kinds of back-ends (C#, Ruby, Spring Boot, Ocaml +++) - https://htmx.org/server-examples/
- Quite similar Kotlin example (referenced from HTMX itself) - https://github.com/Rattlyy/htmx-ktor/tree/master

# Why?

Separate builds for back end and front end is manageable, but gives a worse feedback loop than actually having it all integrated. It is one of the benefits of frameworks like NextJS. But those frameworks also comes with a lot of complexity in the build pipelines, but also in the runtime.

I believe this "stack" gives a simpler environment that is easier to monitor, debug and maintain over time.

It is a big leap from the SPA pages we are used to, but it is worth it. Don't dismiss it before actually trying.

Sometimes you should not use it, but I do think starting with HTMX and moving on when needed is a much better approach than the other way around. HTMX has a really fair and balanced article for when to use it here: https://htmx.org/essays/when-to-use-hypermedia/ 

# Notable parts of the code
- The wrapper method that scaffolds everything with HTML+HEAD+BODY: https://github.com/anderssv/kotlin-htmx/blob/main/src/main/kotlin/no/mikill/kotlin_htmx/pages/HtmlElements.kt#L66
- The back end search function/endpoint that matches and returns HTML response: https://github.com/anderssv/kotlin-htmx/blob/c6b400a86ae0005d99496ec8d655df3a7277d129/src/main/kotlin/no/mikill/kotlin_htmx/pages/MainPage.kt#L26
- The first page full generation (uses the scaffold above): https://github.com/anderssv/kotlin-htmx/blob/c6b400a86ae0005d99496ec8d655df3a7277d129/src/main/kotlin/no/mikill/kotlin_htmx/pages/MainPage.kt#L82
- The base bootstrap for the KTor application: https://github.com/anderssv/kotlin-htmx/blob/main/src/main/kotlin/no/mikill/kotlin_htmx/Application.kt

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

See the possible variables in ```.env.default```, you can copy it to .env.local to do local modifications. Or you can set the variables in env.

## Starting

Run the ```fun main()``` in Application.kt in your favourite IDE, or simply run:

    $ ./gradlew run

It is then available on http://localhost:8080 .

# Other notes
I used KTor generator to get started.

We load _everything_ via ```<script />``` tags to avoid having any kind of build steps for the front end. Tailwind CSS
advises against this for production, but the 355K CDN download can usually be ok for many sites as the rest is small.

## Back end code structure

One of the downsides of this is that there are few "best practices" and recommended structures. But that is
also the benefit. You can use any library or structure you like. For Thymeleaf you will probably get some
recommendations, for others something else.

It is still important to have some kind of defaults for route, controller and pages. YMMV.

This repo contains a kind of naming try at putting pages into objects. I like that as it gives a specific place
to go for each page, but it does overlap a bit with controllers, so you might want to separate those as well.
They could have a common interface for the main things in each page, but I see little benefit in that unless you're 
trying to make some kind of framework. (You shouldn't)

## Bigger scale and functionality?

This is a small example with simple functionality. Performance wise there is no reason this won't scale, but for some features you will add additional Javascript libraries. Something like AlpineJS seems to go well together with HTMX.

People are doing it in production. See this article and presentations for a real world migration from React to HTMX: https://htmx.org/essays/a-real-world-react-to-htmx-port/

# Demo steps
This is a small demo that I do for my presentation "Effective development with Ktor, HTML and HTMX".

- Demo HTMX
    - Simple interface
      - Launch http://0.0.0.0:8080/demo/htmx
      - Wait for the loading indicator
      - Wait for the load
      - Open developer tools
      - Reload
      - Click the button
      - Remove automatic loading
      - Inline the HTML
    - Admin interface
      - Load and click
      - Reload
      - Go to http://0.0.0.0:8080/demo/item/0
      - View source
    - Selection interface
      - Go to http://0.0.0.0:8080
      - Open developer tools
      - Turn on More Tools > Rendering > Paint Flashing
      - Reload
      - Mouse over
      - Click
      - 
- Demo KTor and HTMX
- 

# Possible TODO
- Set up logging with JSON
- Do item repo as example?