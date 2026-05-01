package no.mikill.kotlin_htmx.context

import no.mikill.kotlin_htmx.integration.LookupResult
import no.mikill.kotlin_htmx.registration.Person
import no.mikill.kotlin_htmx.registration.valid
import no.mikill.kotlin_htmx.validation.ValidationResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AppDependenciesTest {
    @Test
    fun `SystemTestContext provides PersonRepository through repositories`() {
        with(SystemTestContext()) {
            val person = Person.valid()
            repositories.personRepository.save(person)

            assertThat(repositories.personRepository.findById(person.id)).isNotNull
        }
    }

    @Test
    fun `SystemTestContext provides ValidationService through services`() {
        with(SystemTestContext()) {
            val person = Person.valid()
            val result = services.validationService.validate(person)

            assertThat(result).isInstanceOf(ValidationResult.Valid::class.java)
        }
    }

    @Test
    fun `SystemTestContext provides LookupClient through clients`() {
        with(SystemTestContext()) {
            val result = clients.lookupClient.lookup("Invalid")

            assertThat(result).isInstanceOf(LookupResult.InvalidInput::class.java)
        }
    }
}
