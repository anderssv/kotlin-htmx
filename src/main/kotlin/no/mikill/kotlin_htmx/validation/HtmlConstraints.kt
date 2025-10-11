package no.mikill.kotlin_htmx.validation

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlinx.html.InputType
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

        // Check for @NotEmpty annotation
        javaField.getAnnotation(NotEmpty::class.java)?.let {
            attributes["required"] = ""
        }

        // Check for @NotNull annotation
        javaField.getAnnotation(NotNull::class.java)?.let {
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

        // Check for @Pattern annotation
        javaField.getAnnotation(Pattern::class.java)?.let { patternAnnotation ->
            attributes["pattern"] = patternAnnotation.regexp
        }

        return attributes
    }

    /**
     * Automatically determines the HTML input type based on property annotations.
     * Currently supports:
     * - @Email -> email
     * - Default -> text
     */
    fun <T, R> getInputType(property: KProperty1<T, R>): InputType {
        val javaField = property.javaField ?: return InputType.text

        // Check for @Email annotation
        javaField.getAnnotation(Email::class.java)?.let {
            return InputType.email
        }

        // Default to text
        return InputType.text
    }
}
