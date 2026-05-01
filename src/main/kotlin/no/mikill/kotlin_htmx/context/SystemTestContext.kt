package no.mikill.kotlin_htmx.context

import jakarta.validation.Validation
import no.mikill.kotlin_htmx.integration.LookupClient
import no.mikill.kotlin_htmx.registration.PersonRepository
import no.mikill.kotlin_htmx.validation.ValidationService

class SystemTestContext : AppDependencies {
    inner class TestRepositories : AppDependencies.Repositories {
        override val personRepository = PersonRepository()
    }

    override val repositories = TestRepositories()

    override val services =
        object : AppDependencies.Services {
            override val validationService =
                ValidationService(
                    Validation.buildDefaultValidatorFactory().validator,
                )
        }

    override val clients =
        object : AppDependencies.Clients {
            override val lookupClient = LookupClient("test-api-key")
        }

    override val numberOfCheckboxes = 10
}
