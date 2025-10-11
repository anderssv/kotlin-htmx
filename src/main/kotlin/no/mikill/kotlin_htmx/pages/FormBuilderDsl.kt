package no.mikill.kotlin_htmx.pages

import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.div
import kotlinx.html.hiddenInput
import kotlinx.html.label
import kotlinx.html.option
import kotlinx.html.select
import no.mikill.kotlin_htmx.validation.PropertyPath
import no.mikill.kotlin_htmx.validation.at
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * DSL builder for creating forms with automatic value binding and validation.
 *
 * This DSL reduces repetition when rendering multiple form fields by:
 * - Automatically binding the value object to all fields
 * - Automatically passing violations to all fields
 * - Providing a concise syntax for field definitions
 *
 * Example:
 * ```
 * personForm(person, violations) {
 *     field(Person::firstName.toPath(), "First Name")
 *     field(Person::lastName.toPath(), "Last Name")
 *     field(Person::email.toPath(), "Email")
 * }
 * ```
 */
class FormBuilder<T>(
    private val valueObject: T,
    private val violations: Map<String, List<String>>,
) {
    /**
     * Renders a form field with automatic value extraction and validation.
     * The field is automatically wrapped in a div.
     *
     * @param propertyPath The type-safe property path
     * @param label The label text for the input
     * @param inputType Optional: Override the automatically determined input type
     * @param placeholder Optional: Placeholder text
     * @param cssClasses Optional: CSS classes to apply
     * @param inputId Optional: HTML id attribute
     */
    fun <R> FlowContent.field(
        propertyPath: PropertyPath<T, R>,
        label: String,
        inputType: InputType? = null,
        placeholder: String? = null,
        cssClasses: String? = null,
        inputId: String? = null,
    ) {
        div {
            validatedInputWithErrors(
                propertyPath = propertyPath,
                valueObject = valueObject,
                violations = violations,
                label = label,
                inputType = inputType,
                placeholder = placeholder,
                cssClasses = cssClasses,
                inputId = inputId,
            )
        }
    }
}

/**
 * Creates a generic form builder DSL for rendering any object's properties.
 *
 * This function works with any object type, making it reusable for persons,
 * products, or any other domain objects.
 *
 * Example:
 * ```
 * form(person, violations) {
 *     field(Person::firstName.toPath(), "First Name")
 *     field(Person::lastName.toPath(), "Last Name")
 * }
 * ```
 *
 * @param valueObject The object containing the form data
 * @param violations Map of validation violations to display
 * @param block The DSL block defining form fields
 */
fun <T> FlowContent.form(
    valueObject: T,
    violations: Map<String, List<String>>,
    block: FormBuilder<T>.() -> Unit,
) {
    val builder = FormBuilder(valueObject, violations)
    builder.apply { block() }
}

/**
 * DSL builder for creating forms with indexed properties (e.g., addresses[0]).
 *
 * This builder simplifies working with list elements by:
 * - Automatically building indexed paths like `Person::addresses.at(index, Address::city)`
 * - Extracting values from the correct list element
 * - Handling validation errors for indexed properties
 *
 * Example:
 * ```
 * indexedForm(person, violations, Person::addresses, 0) {
 *     field(Address::streetAddress, "Street Address")
 *     field(Address::city, "City")
 * }
 * ```
 */
class IndexedFormBuilder<T, E>(
    private val valueObject: T,
    private val violations: Map<String, List<String>>,
    private val listProperty: KProperty1<T, List<E>>,
    private val index: Int,
) {
    /**
     * Renders a form field for an indexed property with automatic path building and value extraction.
     * The field is automatically wrapped in a div.
     *
     * @param elementProperty The property on the list element type (e.g., Address::city)
     * @param label The label text for the input
     * @param inputType Optional: Override the automatically determined input type
     * @param placeholder Optional: Placeholder text
     * @param cssClasses Optional: CSS classes to apply
     * @param inputId Optional: HTML id attribute
     */
    fun <R> FlowContent.field(
        elementProperty: KProperty1<E, R>,
        label: String,
        inputType: InputType? = null,
        placeholder: String? = null,
        cssClasses: String? = null,
        inputId: String? = null,
    ) {
        val propertyPath = listProperty.at(index, elementProperty)
        div {
            validatedInputWithErrors(
                propertyPath = propertyPath,
                valueObject = valueObject,
                violations = violations,
                label = label,
                inputType = inputType,
                placeholder = placeholder,
                cssClasses = cssClasses,
                inputId = inputId,
            )
        }
    }

    /**
     * Renders a hidden input field for an indexed property with automatic path building and value extraction.
     *
     * @param elementProperty The property on the list element type (e.g., Address::id)
     */
    fun <R> FlowContent.hiddenField(elementProperty: KProperty1<E, R>) {
        val propertyPath = listProperty.at(index, elementProperty)
        val currentValue = propertyPath.getValue(valueObject)

        hiddenInput {
            name = propertyPath.path
            value = currentValue?.toString() ?: ""
        }
    }

    /**
     * Renders an enum dropdown for an indexed property with automatic path building and value selection.
     * The dropdown is automatically wrapped in a div.
     *
     * Enum values are automatically extracted from the property's return type using reflection.
     *
     * @param elementProperty The enum property on the list element type (e.g., Address::type)
     * @param labelText The label text for the dropdown
     * @param cssClasses Optional: CSS classes to apply to the select element
     */
    @Suppress("UNCHECKED_CAST")
    fun <R : Enum<R>> FlowContent.enumSelect(
        elementProperty: KProperty1<E, R?>,
        labelText: String,
        cssClasses: String? = null,
    ) {
        val propertyPath = listProperty.at(index, elementProperty)
        val currentValue = propertyPath.getValue(valueObject)

        // Extract enum class from property return type using reflection
        val enumClass =
            (elementProperty.returnType.classifier as? KClass<*>)
                ?.let { if (it.java.isEnum) it.java as Class<R> else null }
                ?: throw IllegalArgumentException("Property ${elementProperty.name} must be an enum type")

        val enumValues = enumClass.enumConstants

        div {
            label {
                +labelText
                select(classes = cssClasses) {
                    name = propertyPath.path
                    enumValues.forEach { enumValue ->
                        option {
                            value = enumValue.name
                            if (enumValue == currentValue) {
                                selected = true
                            }
                            +enumValue.name
                        }
                    }
                }
            }
            // Render validation errors
            violations[propertyPath.path]?.forEach { error ->
                div(classes = "form-error") {
                    +error
                }
            }
        }
    }
}

/**
 * Creates a generic form builder DSL for rendering indexed list properties.
 *
 * This function works with any list property on any object type, making it
 * reusable for addresses, phone numbers, or any other list-based form fields.
 *
 * Example:
 * ```
 * indexedForm(person, violations, Person::addresses, 0) {
 *     field(Address::streetAddress, "Street Address")
 *     field(Address::city, "City")
 * }
 * ```
 *
 * @param valueObject The root object containing the list property
 * @param violations Map of validation violations to display
 * @param listProperty The property reference to the list (e.g., Person::addresses)
 * @param index The index of the element in the list
 * @param block The DSL block defining element fields
 */
fun <T, E> FlowContent.indexedForm(
    valueObject: T,
    violations: Map<String, List<String>>,
    listProperty: KProperty1<T, List<E>>,
    index: Int,
    block: IndexedFormBuilder<T, E>.() -> Unit,
) {
    val builder =
        IndexedFormBuilder(
            valueObject = valueObject,
            violations = violations,
            listProperty = listProperty,
            index = index,
        )
    builder.apply { block() }
}
