package edu.mmaltsau.interviews.domain.dictionary.service

import edu.mmaltsau.interviews.domain.dictionary.model.Counter
import edu.mmaltsau.interviews.domain.dictionary.repository.CounterRepository
import edu.mmaltsau.interviews.server.exceptions.DomainException
import edu.mmaltsau.interviews.server.exceptions.RepositoryInsertConflictException
import edu.mmaltsau.interviews.server.exceptions.ResourceAlreadyExistsException
import edu.mmaltsau.interviews.server.exceptions.ResourceNotFoundException
import io.ktor.server.plugins.BadRequestException
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

interface CounterService {
    suspend fun getAll(): List<Counter>

    suspend fun get(name: String): Counter

    suspend fun create(name: String, initialValue: Int?): Counter

    suspend fun delete(name: String): Int

    suspend fun increment(name: String, value: Int?): Counter

    suspend fun incrementUnsafe(name: String, incrementValue: Int?): Counter
}

class CounterServiceImpl(private val counterRepository: CounterRepository) : CounterService {
    companion object {
        private val log = LoggerFactory.getLogger(CounterServiceImpl::class.java)
    }

    override suspend fun getAll(): List<Counter> {
        return counterRepository.findAll()
    }

    override suspend fun get(name: String): Counter {
        return counterRepository.find(name)
            ?: throw ResourceNotFoundException("Resource not found: $name")
    }

    override suspend fun create(
        name: String,
        initialValue: Int?
    ): Counter {
        try {
            return counterRepository.insert(name, initialValue ?: 0)
        } catch (e: RepositoryInsertConflictException) {
            throw ResourceAlreadyExistsException("Resource already exists: $name", e)
        }
    }

    override suspend fun delete(name: String): Int {
        return counterRepository.delete(name)
    }

    override suspend fun increment(
        name: String,
        value: Int?
    ): Counter {
        if (value == null) {
            throw BadRequestException("Missing increment value")
        }
        return counterRepository.increaseValue(name, value)
            ?: throw ResourceNotFoundException("Resource counter $name was not updated")
    }

    override suspend fun incrementUnsafe(
        name: String,
        incrementValue: Int?
    ): Counter {
        if (incrementValue == null) {
            throw BadRequestException("Missing increment value")
        }

        val counter =
            counterRepository.find(name) ?: throw ResourceNotFoundException("Resource counter $name is not found")

        // artificial delay to simulate lost update scenario
        delay(10.milliseconds)

        val newValue = counter.value + incrementValue
        log.info("Setting value {} for counter {}", newValue, name)
        return counterRepository.update(name, newValue)
            ?: throw DomainException("Unexpected error while updating counter $name")
    }
}