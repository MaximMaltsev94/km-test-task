package edu.mmaltsau.interviews

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class ClientAppScenarios(
    private val countersV1RestClient: CountersV1RestClient
) {

    companion object {
        private val log = LoggerFactory.getLogger(ClientAppScenarios::class.java)
    }


    suspend fun printAllCounters() {
        val allCounters = countersV1RestClient.getAll()

        log.info("Received {} counters", allCounters.size)

        allCounters.forEach {
            log.info("Counter {} - {}", it.name, it.value)
        }

    }

    suspend fun concurrentIncrementSafe() {
        log.info("Running scenario for API that has correct concurrency handling")
        this.concurrentIncrementScenarioInternal(
            countersV1RestClient::increment
        )
    }

    suspend fun concurrentIncrementUnsafe() {
        log.info("Running scenario for API that has NO correct concurrency handling")
        this.concurrentIncrementScenarioInternal(
            countersV1RestClient::incrementUnsafe
        )
    }

    suspend fun concurrentInsert() {
        val concurrentCreateScope = CoroutineScope(Job() + Dispatchers.IO)

        val counterName = UUID.randomUUID().toString()

        val concurrency = 300

        log.info("Going to create counter {} in database {} times simultaneously", counterName, concurrency)

        val coroutines = (1..concurrency).map {
            concurrentCreateScope.async {
                val initialValue = Random.nextInt(100, 1000)
                try {
                    countersV1RestClient.create(counterName, initialValue)

                    log.info("Successfully created counter #{} with initial value {}", it, initialValue)
                    return@async 1
                } catch (e: Exception) {
                    log.info("Failed to create counter #{} with initial value {}", it, initialValue)
                    return@async 0
                }
            }
        }

        val executionResults = coroutines.awaitAll()

        log.info("Finished executing coroutines with total successful executions: {}", executionResults.sum())
    }


    private suspend fun concurrentIncrementScenarioInternal(
        incrementApiCall: suspend (counterName: String, incrementValue: Int) -> Unit
    ) {

        val concurrentIncrementsScope = CoroutineScope(Job() + Dispatchers.IO)


        val counterName = UUID.randomUUID().toString()
        val initialValue = Random.nextInt(100, 1000)

        val newCounter = countersV1RestClient.create(counterName, initialValue)

        log.info("Initial counter {} - {}", counterName, newCounter.value.toString())

        val numAttempts = 100
        val operationsDelaySeconds = 1.seconds
        val concurrency = 300
        val incrementValue = 1
        log.info(
            """
            Incrementing counter concurrently:
            times:          {} increment cycles
            concurrency:    {} concurrent requests per cycle
            incrementValue: {}
            estimatedTime:  {} seconds
        """.trimIndent(),
            numAttempts, concurrency, incrementValue, numAttempts * operationsDelaySeconds.toInt(DurationUnit.SECONDS)
        )

        (1..numAttempts).forEach {
            val latchSignal = CompletableDeferred<Unit>()

            val coroutines = (1..concurrency).map {
                concurrentIncrementsScope.async {
                    latchSignal.await()
                    incrementApiCall(counterName, incrementValue)
                }
            }

            latchSignal.complete(Unit)
            coroutines.awaitAll()
            printCompletionPercent(it, numAttempts)
            delay(1.seconds)

        }


        val incrementedCounter = countersV1RestClient.get(counterName)

        val expectedIncrement = numAttempts * concurrency * incrementValue
        val expectedValue = initialValue + expectedIncrement
        log.info(
            "Incremented counter {}: \n " +
                    "initialValue:       {} \n" +
                    "expectedIncrement:  {} \n" +
                    "expectedValue:      {} \n" +
                    "actualValue:        {} \n",
            counterName, initialValue, expectedIncrement, expectedValue, incrementedCounter.value
        )

    }

    private fun printCompletionPercent(current: Int, total: Int, barLength: Int = 10) {
        if (total <= 0) return

        // Calculate percentage and number of filled characters
        val percentage = (current.toDouble() / total * 100).coerceIn(0.0, 100.0).toInt()
        val filledLength = (current.toDouble() / total * barLength).coerceIn(0.0, barLength.toDouble()).toInt()

        // Create the visual bar
        val bar = "#".repeat(filledLength) + "-".repeat(barLength - filledLength)

        // \r moves the cursor back to the start of the line
        log.info("Current progress: [{}], {}%", bar, percentage)

    }
}

