package no.mikill.kotlin_htmx

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

fun getValueFromPath(obj: Any, path: String): Any? {
    val pathParts = path.split(".")
    var currentObj: Any = obj

    for (part in pathParts) {
        val arrayMatch = Regex("""(\w+)\[(\d+)]""").matchEntire(part)
        currentObj = if (arrayMatch != null) {
            val propName = arrayMatch.groupValues[1]
            val index = arrayMatch.groupValues[2].toInt()
            val property = currentObj.javaClass.kotlin.memberProperties.find { it.name == propName }
            val list = property?.get(currentObj) as? List<*>
            list?.get(index) ?: return null
        } else {
            val property = currentObj.javaClass.kotlin.memberProperties.find { it.name == part }
            property?.get(currentObj) ?: return null
        }
    }

    return currentObj
}

inline fun <reified T : Any> getProperty(fieldPath: String): KProperty1<out Any, *> {
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
