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
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class DataHandlingTest {

    @Test
    fun shouldValidateObject() {
        val badPerson = Application.valid().let { it.copy(person = it.person.copy(firstName = "")) }
        val validator = Validation.buildDefaultValidatorFactory().validator
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
    fun shouldGetPropertyAndValue() {
        val application = Application.valid()

        resolveProperty<String>(application, "person.firstName").let { propertyAndValue ->
            assertThat(propertyAndValue.value).isEqualTo("Ola")
            assertThat(propertyAndValue.property.javaField?.annotations?.map { it.annotationClass }).contains(NotEmpty::class)
        }
    }

    @Test
    fun shouldGetFieldAndValue() {
        val annotations = getField<Application>("person.firstName").javaField?.annotations
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

fun getValueFromPath(obj: Any?, path: String): Any? {
    if (obj == null) return null

    val pathParts = path.split(".")
    var currentObj: Any? = obj

    for (part in pathParts) {
        val arrayMatch = Regex("""(\w+)\[(\d+)]""").matchEntire(part)
        currentObj = if (arrayMatch != null) {
            val propName = arrayMatch.groupValues[1]
            val index = arrayMatch.groupValues[2].toInt()
            val property = currentObj?.javaClass?.kotlin?.memberProperties?.find { it.name == propName }
            val list = currentObj?.let { property?.get(it) } as? List<*>
            list?.get(index)
        } else {
            val property = currentObj?.javaClass?.kotlin?.memberProperties?.find { it.name == part }
            if (currentObj != null) {
                property?.get(currentObj)
            } else null
        }
    }

    return currentObj
}

inline fun <reified T : Any> getField(fieldPath: String): KProperty1<out Any, *> {
    val fieldParts = fieldPath.split(".")
    var currentClass: KClass<*> = T::class

    for (i in 0 until fieldParts.size - 1) {
        val property = currentClass.memberProperties.firstOrNull { it.name == fieldParts[i] }
            ?: throw IllegalArgumentException("No property named '${fieldParts[i]}' found in class ${currentClass.simpleName}")
        currentClass = property.returnType.classifier as? KClass<*>
            ?: throw IllegalArgumentException("Property '${fieldParts[i]}' is not a class in ${currentClass.simpleName}")
    }

    return currentClass.memberProperties.firstOrNull { it.name == fieldParts.last() }
        ?: throw IllegalArgumentException("No property named '${fieldParts.last()}' found in class ${currentClass.simpleName}")
}

private fun Application.Companion.valid(): Application {
    return Application(UUID.randomUUID(), Person("Ola", "Nordmann"), "Comment")
}