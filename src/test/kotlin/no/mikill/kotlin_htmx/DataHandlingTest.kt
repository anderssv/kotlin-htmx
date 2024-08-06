package no.mikill.kotlin_htmx

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.validation.Validation
import jakarta.validation.constraints.NotEmpty
import no.mikill.kotlin_htmx.application.Application
import no.mikill.kotlin_htmx.application.Person
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*
import kotlin.reflect.jvm.javaField

class DataHandlingTest {

    @Test
    fun shouldValidateObject() {
        val badPerson = Application.valid().let { it.copy(person = it.person.copy(firstName = "")) }
        val validator = Validation.buildDefaultValidatorFactory().validator
        val errors = validator.validate(badPerson)
        assertThat(errors).isNotEmpty
        errors.filter { it.propertyPath.toString() == "person.firstName" }.let { filteredViolationsList ->
            assertThat(filteredViolationsList).isNotEmpty
            assertThat(filteredViolationsList.map { it.message }).contains("must not be empty")
        }
    }

    @Test
    fun shouldParseJson() {
        val applicationJson = Application.valid().let {
            """
                {
                    "id": "${UUID.randomUUID()}",
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
        assertThat(application.person.firstName).isEqualTo("Ola")
    }

    @Test
    fun shouldGenerateJson() {
        val application = Application.valid()
        val applicationJson =
            ObjectMapper().registerModule(KotlinModule.Builder().build()).writeValueAsString(application)
        println(applicationJson)
        assertThat(applicationJson).isEqualTo("""{"id":"${application.id}","person":{"firstName":"Ola","lastName":"Nordmann"},"comments":"Comment"}""")
    }

    @Test
    fun shouldGetFieldAndValue() {
        val annotations = getProperty<Application>("person.firstName").javaField?.annotations
        assertThat(annotations?.map { it.annotationClass }).contains(NotEmpty::class)

        val application = Application.valid()
        val value = getValueFromPath(application, "person.firstName")
        assertThat(value).isEqualTo("Ola")
    }


    @Test
    fun shouldUpdatePropertyOnObject() {
        val application = Application.valid()

        val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val newApplication: Application =
            mapper.readerForUpdating(application).readValue("""{"person":{"firstName":"Kari"}}""")
        assertThat(newApplication.person.firstName).isEqualTo("Kari")
        assertThat(newApplication.person.lastName).isEqualTo("Nordmann")
        assertThat(newApplication.comments).isEqualTo("Comment")
    }
}

private fun Application.Companion.valid(): Application {
    return Application(UUID.randomUUID(), Person("Ola", "Nordmann"), "Comment")
}