package edu.mmaltsau.interviews.domain.dictionary.service

import edu.mmaltsau.interviews.domain.dictionary.model.Counter
import edu.mmaltsau.interviews.domain.dictionary.repository.CounterRepository
import edu.mmaltsau.interviews.server.exceptions.RepositoryInsertConflictException
import edu.mmaltsau.interviews.server.exceptions.ResourceAlreadyExistsException
import edu.mmaltsau.interviews.server.exceptions.ResourceNotFoundException
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

interface CounterService {
    suspend fun getAll(): List<Counter>

    suspend fun get(name: String): Counter

    suspend fun create(name: String, initialValue: Int?): Counter

    suspend fun delete(name: String): Int

    suspend fun increment(name: String, value: Int?): Int

    suspend fun incrementUnsafe(name: String, incrementValue: Int?): Int
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
    ): Int {
        if (value == null) {
            return 0
        }
        val modifiedCount = counterRepository.increaseValue(name, value)
        if (modifiedCount == 0) {
            throw ResourceNotFoundException("Resource counter $name was not updated")
        }
        return modifiedCount
    }

    override suspend fun incrementUnsafe(
        name: String,
        incrementValue: Int?
    ): Int {
        if (incrementValue == null) {
            return 0
        }

        val counter = counterRepository.find(name) ?: throw ResourceNotFoundException("Resource counter $name is not found")

        // artificial delay to simulate lost update scenario
        delay(10.milliseconds)

        val newValue = counter.value + incrementValue
        log.info("Setting value {} for counter {}", newValue, name)
        return counterRepository.update(name, newValue)
    }
}