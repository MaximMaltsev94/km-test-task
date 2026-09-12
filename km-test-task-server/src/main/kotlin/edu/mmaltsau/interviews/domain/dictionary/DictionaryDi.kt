package edu.mmaltsau.interviews.domain.dictionary

import edu.mmaltsau.interviews.domain.dictionary.repository.CounterRepository
import edu.mmaltsau.interviews.domain.dictionary.repository.PostgresqlCounterRepository
import edu.mmaltsau.interviews.domain.dictionary.service.CounterService
import edu.mmaltsau.interviews.domain.dictionary.service.CounterServiceImpl
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies

fun Application.dictionaryDi() {

    dependencies {

        provide<CounterRepository>(PostgresqlCounterRepository::class)
        provide<CounterService>(CounterServiceImpl::class)

    }
}