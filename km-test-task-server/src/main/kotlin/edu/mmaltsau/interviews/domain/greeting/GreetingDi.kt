package edu.mmaltsau.interviews.domain.greeting

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies


fun Application.greetingDi() {
    dependencies {
        provide { StaticGreetingService() }
    }
}
