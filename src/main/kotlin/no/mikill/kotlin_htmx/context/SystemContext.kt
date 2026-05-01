package no.mikill.kotlin_htmx.context

import jakarta.validation.Validation
import no.mikill.kotlin_htmx.ApplicationConfig
import no.mikill.kotlin_htmx.integration.LookupClient
import no.mikill.kotlin_htmx.registration.PersonRepository
import no.mikill.kotlin_htmx.validation.ValidationService

class SystemContext(
    private val config: ApplicationConfig,
    override val numberOfCheckboxes: Int,
) : AppDependencies {
    override val repositories =
        object : AppDependencies.Repositories {
            override val personRepository = PersonRepository()
        }

    override val services =
        object : AppDependencies.Services {
            override val validationService =
                ValidationService(
                    Validation.buildDefaultValidatorFactory().validator,
                )
        }

    override val clients =
        object : AppDependencies.Clients {
            override val lookupClient = LookupClient(config.lookupApiKey)
        }
}
