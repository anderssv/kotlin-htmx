package no.mikill.kotlin_htmx.application

import java.util.*

class ApplicationRepository {
    private val applications = mutableMapOf<UUID, Application>()

    fun addApplication(application: Application) {
        applications[application.id] = application
    }

    fun getApplication(id: UUID): Application? {
        return applications[id]
    }
}