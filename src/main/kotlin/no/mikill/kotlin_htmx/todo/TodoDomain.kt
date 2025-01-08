package no.mikill.kotlin_htmx.todo

data class TodoListItem(
    val id: Int, val title: String, val completed: Boolean
)

// Would normally be in the DB
val todoListItems = listOf(
    TodoListItem(1, "Buy milk", false),
    TodoListItem(2, "Buy bread", false),
    TodoListItem(3, "Buy eggs", false),
    TodoListItem(4, "Buy butter", false)
)
