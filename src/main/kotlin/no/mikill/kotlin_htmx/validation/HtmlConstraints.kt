package no.mikill.kotlin_htmx.validation

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField

object HtmlConstraints {
    fun <T, R> getAttributes(property: KProperty1<T, R>): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        val javaField = property.javaField ?: return emptyMap()

        // Check for @NotBlank annotation
        javaField.getAnnotation(NotBlank::class.java)?.let {
            attributes["required"] = ""
        }

        // Check for @Size annotation
        javaField.getAnnotation(Size::class.java)?.let { sizeAnnotation ->
            if (sizeAnnotation.max < Int.MAX_VALUE) {
                attributes["maxlength"] = sizeAnnotation.max.toString()
            }
            if (sizeAnnotation.min > 0) {
                attributes["minlength"] = sizeAnnotation.min.toString()
            }
        }

        return attributes
    }
}
