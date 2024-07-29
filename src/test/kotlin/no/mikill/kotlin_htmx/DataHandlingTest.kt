package no.mikill.kotlin_htmx

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.validation.Validation
import no.mikill.kotlin_htmx.application.Application
import no.mikill.kotlin_htmx.application.Person
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DataHandlingTest {

    @Test
    fun shouldValidateObject() {
        val badPerson = Application.valid().let { it.copy(person = it.person.copy(firstName = "")) }
        val validator =Validation.buildDefaultValidatorFactory().validator
        val errors = validator.validate(badPerson)
        assertThat(errors).isNotEmpty
        errors.filter { it.propertyPath.toString() == "person.firstName" }.let {
            assertThat(it).isNotEmpty
            assertThat(it.map { it.message }).contains("must not be empty")
        }
    }

    @Test
    fun shouldParseJson() {
        val applicationJson = Application.valid().let {
            """
                {
                    "person": {
                        "firstName": "${it.person.firstName}",
                        "lastName": "${it.person.lastName}"
                    },
                    "comments": "${it.comments}"
                }
            """.trimIndent()

        }
        val application = ObjectMapper().registerModule(KotlinModule.Builder().build())
            .readValue(applicationJson, Application::class.java)
        println(application)
        assertThat(application.person.firstName).isEqualTo("Ola")
    }

    @Test
    fun shouldGenerateJson() {
        val application = Application.valid()
        val applicationJson =
            ObjectMapper().registerModule(KotlinModule.Builder().build()).writeValueAsString(application)
        println(applicationJson)
        assertThat(applicationJson).isEqualTo("""{"person":{"firstName":"Ola","lastName":"Nordmann"},"comments":""}""")
    }

}


private fun Application.Companion.valid(): Application {
    return Application(Person("Ola", "Nordmann"), "")
}
