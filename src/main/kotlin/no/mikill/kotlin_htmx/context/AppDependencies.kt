package no.mikill.kotlin_htmx.context

import no.mikill.kotlin_htmx.integration.LookupClient
import no.mikill.kotlin_htmx.registration.PersonRepository
import no.mikill.kotlin_htmx.validation.ValidationService

interface AppDependencies {
    interface Repositories {
        val personRepository: PersonRepository
    }

    interface Services {
        val validationService: ValidationService
    }

    interface Clients {
        val lookupClient: LookupClient
    }

    val repositories: Repositories
    val services: Services
    val clients: Clients
    val numberOfCheckboxes: Int
}
