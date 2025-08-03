package no.mikill.kotlin_htmx

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.validation.Validation
import jakarta.validation.constraints.NotEmpty
import no.mikill.kotlin_htmx.application.Application
import no.mikill.kotlin_htmx.application.Person
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.reflect.jvm.javaField

class DataHandlingTest {
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun shouldValidateObject() {
        val badPerson = Application.valid().let { it.copy(person = it.person.copy(firstName = "")) }
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
        val application = objectMapper.readValue(applicationJson, Application::class.java)
        assertThat(application.person.firstName).isEqualTo("Ola")
    }

    @Test
    fun shouldGenerateJson() {
        val application = Application.valid()
        val applicationJson = objectMapper.writeValueAsString(application)
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

        val newApplication: Application =
            objectMapper.readerForUpdating(application).readValue("""{"person":{"firstName":"Kari"}}""")
        assertThat(newApplication.person.firstName).isEqualTo("Kari")
        assertThat(newApplication.person.lastName).isEqualTo("Nordmann")
        assertThat(newApplication.comments).isEqualTo("Comment")
    }
}

private fun Application.Companion.valid(): Application {
    return Application(UUID.randomUUID(), Person("Ola", "Nordmann"), "Comment")
}