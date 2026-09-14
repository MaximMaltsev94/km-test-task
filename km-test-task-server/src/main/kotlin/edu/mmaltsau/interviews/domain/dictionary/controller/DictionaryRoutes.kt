package edu.mmaltsau.interviews.domain.dictionary.controller

import edu.mmaltsau.interviews.domain.dictionary.dto.CounterCreateRequestDto
import edu.mmaltsau.interviews.domain.dictionary.dto.CounterIncrementRequestDto
import edu.mmaltsau.interviews.domain.dictionary.model.Counter
import edu.mmaltsau.interviews.domain.dictionary.service.CounterService
import edu.mmaltsau.interviews.server.ErrorResponse
import edu.mmaltsau.interviews.server.exceptions.MissingRequestBodyException
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

private const val DICTIONARY_V1_API = "api/v1/dictionaries/default"
private const val COUNTERS_API = "/counters"
private const val COUNTER_RESOURCE = "{counterName}"
private const val COUNTER_NAME_PARAM = "counterName"

fun Application.dictionaryRoutes() {

    val counterService: CounterService by dependencies

    routing {
        route(DICTIONARY_V1_API) {
            route(COUNTERS_API) {
                get {
                    val allCounters = counterService.getAll();
                    call.respond(HttpStatusCode.OK, allCounters)
                }.openapiGetCounters()

                get(COUNTER_RESOURCE) {
                    val counterName = call.parameters[COUNTER_NAME_PARAM] ?: throw NotFoundException()
                    val requestedCounter = counterService.get(counterName)
                    call.respond(HttpStatusCode.OK, requestedCounter)
                }.openapiGetCounterByName()

                put(COUNTER_RESOURCE) {
                    val counterName = call.parameters[COUNTER_NAME_PARAM]
                        ?: throw BadRequestException("Required path parameter counterName is missing")

                    val createRequest = call.receiveNullable<CounterCreateRequestDto>()
                        ?: throw MissingRequestBodyException("")

                    val newCounter = counterService.create(counterName, createRequest.initialValue)

                    call.respond(HttpStatusCode.OK, newCounter)
                }.openapiCreateCounter()


                delete(COUNTER_RESOURCE) {
                    val counterName = call.parameters[COUNTER_NAME_PARAM] ?: throw NotFoundException()

                    val deletedCount = counterService.delete(counterName)

                    if (deletedCount > 0) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NoContent)
                    }

                }.openapiDeleteCounter()


                post("/$COUNTER_RESOURCE/increments") {
                    val counterName = call.parameters[COUNTER_NAME_PARAM] ?: throw NotFoundException()

                    val incrementDto = call.receiveNullable<CounterIncrementRequestDto>()
                        ?: throw MissingRequestBodyException("")

                    log.info("Incrementing counter {} by {}", counterName, incrementDto.count)
                    val updatedCounter = counterService.increment(counterName, incrementDto.count)
                    call.respond(HttpStatusCode.OK, updatedCounter)

                }.openapiIncrementCounter()

                post("/$COUNTER_RESOURCE/increments-unsafe") {
                    // endpoint for test purposes to demonstrate concurrent increment errors

                    val counterName = call.parameters[COUNTER_NAME_PARAM] ?: throw NotFoundException()

                    val incrementDto = call.receiveNullable<CounterIncrementRequestDto>()
                        ?: throw MissingRequestBodyException("")

                    log.info("Incrementing counter {} by {}", counterName, incrementDto.count)
                    val updatedCounter = counterService.incrementUnsafe(counterName, incrementDto.count)
                    call.respond(HttpStatusCode.OK, updatedCounter)
                }.openapiIncrementCounterUnsafe()

            }

        }
    }
}
private fun Route.openapiGetCounters() {
    describe {
        summary = "Get all counters in the dictionary"
        tag("counters")
        responses {
            HttpStatusCode.OK {
                description = "List of all counters. Empty list if dictionary has no counters"
                schema = jsonSchema<Counter>()
            }
            HttpStatusCode.NotFound {
                description = "Dictionary resource not found"
                schema = jsonSchema<ErrorResponse>()
            }
            HttpStatusCode.InternalServerError {
                description = "Internal server error"
                schema = jsonSchema<ErrorResponse>()
            }
        }
    }

}

private fun Route.openapiGetCounterByName() {
    describe {
        summary = "Get counter by name"
        tag("counters")
        responses {
            HttpStatusCode.OK {
                description = "Counter object for the requested resource"
                schema = jsonSchema<Counter>()
            }
            HttpStatusCode.NotFound {
                description = "Counter resource not found"
                schema = jsonSchema<ErrorResponse>()
            }
            HttpStatusCode.InternalServerError {
                description = "Internal server error"
                schema = jsonSchema<ErrorResponse>()
            }
        }
    }

}

private fun Route.openapiCreateCounter() {
    describe {
        summary = "Create a new counter"
        description = "Creates a counter with the specified name. Does not allow override existing value."
        tag("counters")
        responses {
            HttpStatusCode.OK {
                description = "Counter was created"
                schema = jsonSchema<Counter>()
            }
            HttpStatusCode.BadRequest {
                description = "Missing request body payload"
                schema = jsonSchema<ErrorResponse>()
            }
            HttpStatusCode.Conflict {
                description = "Counter with the specified name already exists"
                schema = jsonSchema<ErrorResponse>()
            }
            HttpStatusCode.InternalServerError {
                description = "Internal server error"
                schema = jsonSchema<ErrorResponse>()
            }
        }
    }

}

private fun Route.openapiDeleteCounter() {
    describe {
        summary = "Delete a counter by name"
        tag("counters")
        responses {
            HttpStatusCode.OK {
                description = "Counter with the specified name was deleted. Response body is empty."
            }
            HttpStatusCode.NoContent {
                description =
                    "No delete action was performed on server. Possibly resource does not exist. Response body is empty."
            }
            HttpStatusCode.NotFound {
                description = "Resource didn't match handler"
                schema = jsonSchema<ErrorResponse>()
            }
            HttpStatusCode.InternalServerError {
                description = "Internal server error"
                schema = jsonSchema<ErrorResponse>()
            }
        }
    }

}

private fun Route.openapiIncrementCounter() {
    describe {
        summary = "Increment a counter for the specified value"
        description = """
                    Concurrency safe operation. lost update is mitigated 
                    - by read commited transaction isolation level
                    - select for update pessimistic row level locking
                """.trimIndent()
        tag("counters")
        responses {
            HttpStatusCode.OK {
                description = "Counter was incremented"
            }
            HttpStatusCode.BadRequest {
                description = "Missing request body or increment value"
                schema = jsonSchema<ErrorResponse>()
            }
            HttpStatusCode.InternalServerError {
                description = "Internal server error"
                schema = jsonSchema<ErrorResponse>()
            }

        }
    }

}

private fun Route.openapiIncrementCounterUnsafe() {
    describe {
        summary = "Increment a counter for the specified value"
        description = """
                   For demonstration purposes.
                   Concurrency Unsafe operation. Lost update is simulated by two sql statements and application logic
                   1. select in separate transaction
                   2. increment in application
                   3. update in separate transaction
                """.trimIndent()
        tag("counters")

        responses {
            HttpStatusCode.OK {
                description = "Counter was incremented"
            }
            HttpStatusCode.BadRequest {
                description = "Missing request body or increment value"
                schema = jsonSchema<ErrorResponse>()
            }
            HttpStatusCode.NotFound {
                description = "Counter resource is not found"
                schema = jsonSchema<ErrorResponse>()
            }
            HttpStatusCode.InternalServerError {
                description = "Internal server error"
                schema = jsonSchema<ErrorResponse>()
            }

        }
    }

}