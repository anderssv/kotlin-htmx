package no.mikill.kotlin_htmx

val items = listOf(
    SelectionItem("One", "/static/images/groceries.png"),
    SelectionItem("Two", "/static/images/groceries.png"),
    SelectionItem("Three", "/static/images/groceries.png"),
)

data class SelectionItem(
    val name: String,
    val image: String
)