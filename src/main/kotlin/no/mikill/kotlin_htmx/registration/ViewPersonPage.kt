package no.mikill.kotlin_htmx.registration

import kotlinx.html.FlowContent
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.ul

class ViewPersonPage {
    fun renderPersonDetails(
        container: FlowContent,
        person: Person,
    ) {
        container.apply {
            h1 { +"Person Details" }

            p { +"Name: ${person.firstName} ${person.lastName}" }
            p { +"Email: ${person.email}" }

            h2 { +"Addresses" }
            ul {
                person.addresses.forEach { address ->
                    li {
                        +"${address.type}: ${address.streetAddress}, ${address.city}, ${address.postalCode}, ${address.country}"
                    }
                }
            }
        }
    }
}
