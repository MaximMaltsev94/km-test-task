package edu.mmaltsau.interviews.server.exceptions

import io.ktor.server.plugins.BadRequestException

open class DomainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {}

class ResourceNotFoundException(message: String) : DomainException(message) {
}

class ResourceAlreadyExistsException(message: String, cause: Throwable? = null) : DomainException(message, cause) {}

class MissingRequestBodyException(resource: String)
    : BadRequestException("Mandatory request body is missing for the request: $resource") {

}

open class RepositoryException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {}

class RepositoryInsertConflictException(message: String, cause: Throwable? = null) : RepositoryException(message, cause) {}
