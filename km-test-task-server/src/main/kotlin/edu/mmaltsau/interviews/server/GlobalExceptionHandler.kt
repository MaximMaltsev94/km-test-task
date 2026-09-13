package edu.mmaltsau.interviews.server

import edu.mmaltsau.interviews.server.exceptions.DomainException
import edu.mmaltsau.interviews.server.exceptions.ResourceAlreadyExistsException
import edu.mmaltsau.interviews.server.exceptions.ResourceNotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class ErrorResponse(val code: Int, val message: String, val timestampMs: Long)

fun Application.configureExceptionHandling() {

    val log = LoggerFactory.getLogger("GlobalExceptionHandler")
    install(StatusPages) {

        exception<ResourceNotFoundException> { call, cause ->
            log.error("Resource not found", cause)
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    HttpStatusCode.NotFound.value,
                    cause.message ?: "Resource not found",
                    System.currentTimeMillis()
                )
            )
        }
        exception<NotFoundException> { call, cause ->
            log.error("Endpoint not found", cause)
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    HttpStatusCode.NotFound.value,
                    cause.message ?: "Endpoint not found",
                    System.currentTimeMillis()
                )
            )
        }

        exception<ResourceAlreadyExistsException> { call, cause ->
            log.error("Failed to create resource - already exists", cause)
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse(
                    HttpStatusCode.Conflict.value,
                    cause.message ?: "Resource already exists",
                    System.currentTimeMillis()
                )
            )
        }

        exception<DomainException> { call, cause ->
            log.error("Generic domain exception.", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    HttpStatusCode.InternalServerError.value,
                    cause.message ?: "Internal server error",
                    System.currentTimeMillis()
                )
            )
        }

        exception<BadRequestException> { call, cause ->
            log.error("API bad request exception.", cause)
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    HttpStatusCode.BadRequest.value,
                    cause.message ?: "Bad Request",
                    System.currentTimeMillis()
                )
            )
        }

        exception<RuntimeException> { call, cause ->
            log.error("Generic exception.", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    HttpStatusCode.InternalServerError.value,
                    cause.message ?: "Internal server error",
                    System.currentTimeMillis()
                )
            )
        }
    }
}