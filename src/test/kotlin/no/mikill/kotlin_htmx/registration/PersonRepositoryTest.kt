package no.mikill.kotlin_htmx.registration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PersonRepositoryTest {
    @Test
    fun `PersonRepository save stores person and findById retrieves it`() {
        // Arrange
        val repository = PersonRepository()
        val person = Person.valid()

        // Act
        val savedPerson = repository.save(person)
        val retrievedPerson = repository.findById(person.id)

        // Assert
        assertThat(savedPerson).isEqualTo(person)
        assertThat(retrievedPerson).isEqualTo(person)
    }

    @Test
    fun `PersonRepository update modifies existing person`() {
        // Arrange
        val repository = PersonRepository()
        val person = Person.valid()
        repository.save(person)

        // Act
        val updatedPerson =
            person.copy(
                addresses = person.addresses + Address.valid().copy(type = AddressType.WORK),
            )
        repository.save(updatedPerson)
        val retrieved = repository.findById(person.id)

        // Assert
        assertThat(retrieved).isEqualTo(updatedPerson)
        assertThat(retrieved?.addresses).hasSize(1) // Person starts with 0 addresses, adds 1
    }

    @Test
    fun `PersonRepository findAll returns all stored persons`() {
        // Arrange
        val repository = PersonRepository()
        val person1 = Person.valid().copy(firstName = "Alice")
        val person2 = Person.valid().copy(firstName = "Bob")

        // Act
        repository.save(person1)
        repository.save(person2)
        val allPersons = repository.findAll()

        // Assert
        assertThat(allPersons).hasSize(2)
        assertThat(allPersons).containsExactlyInAnyOrder(person1, person2)
    }

    @Test
    fun `PersonRepository findById returns null for non-existent id`() {
        // Arrange
        val repository = PersonRepository()
        val person = Person.valid()

        // Act
        val result = repository.findById(person.id)

        // Assert
        assertThat(result).isNull()
    }
}
