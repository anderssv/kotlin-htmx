package no.mikill.kotlin_htmx.registration

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PersonRepository {
    private val persons = ConcurrentHashMap<UUID, Person>()

    fun save(person: Person): Person {
        persons[person.id] = person
        return person
    }

    fun findById(id: UUID): Person? = persons[id]

    fun findAll(): List<Person> = persons.values.toList()
}
