package no.mikill.kotlin_htmx.application

import com.fasterxml.jackson.annotation.JsonMerge
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class Application(
    @field:JsonMerge
    @field:Valid
    val person: Person,
    val comments: String
) {
    companion object
}

data class Person(
    @field:NotEmpty
    @field:Size(min = 3)
    val firstName: String,
    @field:NotEmpty
    @field:Size(min = 3)
    val lastName: String
)