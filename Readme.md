# A  full stack webapp with Kotlin, KTor and HTMX
- https://ktor.io
- https://htmx.org
- https://tailwindcss.com

Develop full stack, with hot reloading. All you need to see the **full** results of a change is a compile 
of the back end and a reload.

Used KTor generator to get started

We load _everything_ via ```<script />``` tags to avoid having any kind of build steps for the front end. Tailwind CSS
advises against this for production, but the 355K CDN download can usually be ok for many sites as the rest is small.

# Getting to a bigger scale

This is a small example with some examples. But I do believe this track is worthwhile, possibly in combination with
something like AlpineJS.

People are doing it in production. :)

# Local environment

Set environment variables directly or in a .env.local file.

# Back end code structure

One of the downsides of this is that there are few "best practices" and recommended structures. But that is
also the benefit, that you can use any library or structure you like. For Thymeleaf you will probably get some
recommendations, for others something else.

It is still important to have some kind of defaults for route, controller and pages. YMMV.

This repo contains a kind of naming try at putting pages into objects. I kind of like that as it gives a specific place
to go for each page, but it does overlap a bit with controllers so you might want to separate those as well.
They could have a common interface for the main things, but I see little benefit in that unless you're trying to 
make some kind of framework.

# Blog


# TODO
- Set up logging with JSON
- Do item repo as example?