package no.mikill.kotlin_htmx.pages

import kotlinx.html.DIV
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import no.mikill.kotlin_htmx.validation.HtmlConstraints
import no.mikill.kotlin_htmx.validation.PropertyPath

fun <T, R> DIV.validatedInput(
    propertyPath: PropertyPath<T, R>,
    value: String,
    label: String,
    inputType: InputType = InputType.text,
    placeholder: String? = null,
    cssClasses: String? = null,
    inputId: String? = null,
    configure: INPUT.() -> Unit = {},
) {
    val pathString = propertyPath.path

    // Extract the property to get validation annotations
    val property =
        when (propertyPath) {
            is PropertyPath.Direct<*, *> -> propertyPath.property
            is PropertyPath.Nested<*, *, *> -> propertyPath.property
            is PropertyPath.Indexed<*, *, *> -> propertyPath.elementProperty
        }

    val constraints = HtmlConstraints.getAttributes(property)

    label {
        +label
        input(type = inputType, name = pathString, classes = cssClasses) {
            inputId?.let { id = it }
            this.value = value
            placeholder?.let { this.placeholder = it }

            // Apply HTML constraints
            constraints.forEach { (attrName, attrValue) ->
                when (attrName) {
                    "required" -> required = true
                    "maxlength" -> maxLength = attrValue
                    "minlength" -> minLength = attrValue
                    "pattern" -> pattern = attrValue
                }
            }

            configure()
        }
    }
}

fun <T, R> DIV.validatedInputWithErrors(
    propertyPath: PropertyPath<T, R>,
    value: String,
    violations: Map<String, List<String>>,
    label: String,
    inputType: InputType = InputType.text,
    placeholder: String? = null,
    cssClasses: String? = null,
    inputId: String? = null,
    configure: INPUT.() -> Unit = {},
) {
    // Render the input using the existing function
    validatedInput(
        propertyPath = propertyPath,
        value = value,
        label = label,
        inputType = inputType,
        placeholder = placeholder,
        cssClasses = cssClasses,
        inputId = inputId,
        configure = configure,
    )

    // Render error messages for this property
    violations[propertyPath.path]?.forEach { error ->
        div(classes = "error-message") {
            +error
        }
    }
}
