package edu.mmaltsau.interviews.domain.greeting

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.request.requirePathParameter
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing


fun Application.greetingRoutes() {
    routing {
        val greetingService: GreetingService by dependencies
        get("/") {
            call.respondText(greetingService.sayBasicHello())
        }
        get("/{name}") {
            val nameParam = call.requirePathParameter("name")
            call.respond(greetingService.sayPersonalHello(nameParam))
        }
        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
    }

}