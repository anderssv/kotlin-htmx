package no.mikill.kotlin_htmx.validation

import kotlin.reflect.KProperty1

sealed class PropertyPath<T, R> {
    abstract val path: String

    abstract fun getValue(obj: T): R

    data class Direct<T, R>(
        val property: KProperty1<T, R>,
    ) : PropertyPath<T, R>() {
        override val path: String = property.name

        override fun getValue(obj: T): R = property.get(obj)
    }

    data class Nested<T, M, R>(
        val parent: PropertyPath<T, M>,
        val property: KProperty1<M, R>,
    ) : PropertyPath<T, R>() {
        override val path: String = "${parent.path}.${property.name}"

        override fun getValue(obj: T): R {
            val intermediate = parent.getValue(obj)
            return property.get(intermediate)
        }
    }

    data class Indexed<T, E, R>(
        val listProperty: KProperty1<T, List<E>>,
        val index: Int,
        val elementProperty: KProperty1<E, R>,
    ) : PropertyPath<T, R>() {
        override val path: String = "${listProperty.name}[$index].${elementProperty.name}"

        override fun getValue(obj: T): R {
            val list = listProperty.get(obj)
            val element = list[index]
            return elementProperty.get(element)
        }
    }
}

fun <T, R> KProperty1<T, R>.toPath(): PropertyPath<T, R> = PropertyPath.Direct(this)

fun <T, M, R> PropertyPath<T, M>.then(property: KProperty1<M, R>): PropertyPath<T, R> = PropertyPath.Nested(this, property)

fun <T, E, R> KProperty1<T, List<E>>.at(
    index: Int,
    property: KProperty1<E, R>,
): PropertyPath<T, R> = PropertyPath.Indexed(this, index, property)
