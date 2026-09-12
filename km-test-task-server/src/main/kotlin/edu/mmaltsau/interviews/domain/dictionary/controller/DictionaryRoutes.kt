package edu.mmaltsau.interviews.domain.dictionary.controller

import edu.mmaltsau.interviews.domain.dictionary.dto.CounterCreateRequestDto
import edu.mmaltsau.interviews.domain.dictionary.dto.CounterIncrementRequestDto
import edu.mmaltsau.interviews.domain.dictionary.service.CounterService
import edu.mmaltsau.interviews.server.exceptions.MissingRequestBodyException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.dictionaryRoutes() {

    val counterService: CounterService by dependencies

    routing {
        route("api/v1/dictionaries/default") {

            get("/counters") {
                val allCounters = counterService.getAll();
                call.respond(HttpStatusCode.OK, allCounters)
            }.describe {
                summary = "Get all counters in the dictionary"
                tag("counters")
            }

            get("/counters/{counterName}") {
                val counterName = call.parameters["counterName"] ?: throw NotFoundException()
                val requestedCounter = counterService.get(counterName)
                call.respond(HttpStatusCode.OK, requestedCounter)
            }.describe {
                summary = "Get counter by name"
                tag("counters")
            }

            put("/counters/{counterName}") {
                // 409 conflict on already existing resource

                val counterName = call.parameters["counterName"]
                    ?: throw BadRequestException("Required path parameter counterName is missing")

                val createRequest = call.receiveNullable<CounterCreateRequestDto>()
                    ?: throw MissingRequestBodyException("")

                val newCounter = counterService.create(counterName, createRequest.initialValue)

                call.respond(HttpStatusCode.OK, newCounter)
            }.describe {
                    summary = "Create a new counter"
                    responses {
                        HttpStatusCode.Conflict {
                            description = "Counter with the specified name already exists"
                        }
                    }
                    tag("counters")
                }


            delete("/counters/{counterName}") {
                val counterName = call.parameters["counterName"] ?: throw NotFoundException()

                val deletedCount = counterService.delete(counterName)

                if (deletedCount > 0) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }

            }.describe {
                summary = "Delete a counter by name"
                tag("counters")
            }

            post("/counters/{counterName}/increments") {
                val counterName = call.parameters["counterName"] ?: throw NotFoundException()

                val incrementDto = call.receiveNullable<CounterIncrementRequestDto>()
                    ?: throw MissingRequestBodyException("")

                log.info("Incrementing counter {} by {}", counterName, incrementDto.count)
                val updatedCount = counterService.increment(counterName, incrementDto.count)
                if (updatedCount > 0) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }

            }.describe {
                summary = "Increment a counter for the specified value"
                description = """
                    Concurrency safe operation. lost update is mitigated 
                    - by read commited transaction isolation level
                    - select for update pessimistic row level locking
                """.trimIndent()
                tag("counters")
            }

            post("/counters/{counterName}/increments/unsafe") {
                // endpoint for test purposes to demonstrate concurrent increment errors

                val counterName = call.parameters["counterName"] ?: throw NotFoundException()

                val incrementDto = call.receiveNullable<CounterIncrementRequestDto>()
                    ?: throw MissingRequestBodyException("")

                log.info("Incrementing counter {} by {}", counterName, incrementDto.count)
                val updatedCount = counterService.incrementUnsafe(counterName, incrementDto.count)
                if (updatedCount > 0) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }.describe {
                summary = "Increment a counter for the specified value"
                description = """
                   For demonstration purposes.
                   Concurrency Unsafe operation. Lost update is simulated by two sql statements and application logic
                   1. select in separate transaction
                   2. increment in application
                   3. update in separate transaction
                """.trimIndent()
                tag("counters")
            }
        }
    }
}