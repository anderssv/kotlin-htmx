package no.mikill.kotlin_htmx.registration

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PersonRepository {
    private val persons = ConcurrentHashMap<UUID, Person>()

    fun save(person: Person): Person {
        // Assign UUIDs to addresses that don't have them
        val personWithAddressIds =
            person.copy(
                addresses =
                    person.addresses.map { address ->
                        if (address.id == null) {
                            address.copy(id = UUID.randomUUID())
                        } else {
                            address
                        }
                    },
            )
        persons[personWithAddressIds.id] = personWithAddressIds
        return personWithAddressIds
    }

    fun findById(id: UUID): Person? = persons[id]

    fun findAll(): List<Person> = persons.values.toList()
}
