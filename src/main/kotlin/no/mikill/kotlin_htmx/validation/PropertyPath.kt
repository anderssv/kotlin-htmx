package no.mikill.kotlin_htmx.validation

import kotlin.reflect.KProperty1

sealed class PropertyPath<T, R> {
    abstract val path: String

    data class Direct<T, R>(
        val property: KProperty1<T, R>,
    ) : PropertyPath<T, R>() {
        override val path: String = property.name
    }

    data class Nested<T, M, R>(
        val parent: PropertyPath<T, M>,
        val property: KProperty1<M, R>,
    ) : PropertyPath<T, R>() {
        override val path: String = "${parent.path}.${property.name}"
    }

    data class Indexed<T, E, R>(
        val listProperty: KProperty1<T, List<E>>,
        val index: Int,
        val elementProperty: KProperty1<E, R>,
    ) : PropertyPath<T, R>() {
        override val path: String = "${listProperty.name}[$index].${elementProperty.name}"
    }
}

fun <T, R> KProperty1<T, R>.toPath(): PropertyPath<T, R> = PropertyPath.Direct(this)

fun <T, M, R> PropertyPath<T, M>.then(property: KProperty1<M, R>): PropertyPath<T, R> = PropertyPath.Nested(this, property)

fun <T, E, R> KProperty1<T, List<E>>.at(
    index: Int,
    property: KProperty1<E, R>,
): PropertyPath<T, R> = PropertyPath.Indexed(this, index, property)
