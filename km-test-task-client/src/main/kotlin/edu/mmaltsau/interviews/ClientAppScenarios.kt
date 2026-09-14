package edu.mmaltsau.interviews

import edu.mmaltsau.interviews.dto.CounterV1ResponseDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit


data class CreateResult(
    val counterName: String,
    val value: Int,
    val success: Boolean
) {}

data class IncrementResult(
    val counterName: String,
    val value: Int,
    val success: Boolean
)

class ClientAppScenarios(
    private val countersV1RestClient: CountersV1RestClient,
    private val scenarioDispatcher: CoroutineDispatcher = Dispatchers.IO
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

    suspend fun concurrentInsert() = coroutineScope {

        val counterName = UUID.randomUUID().toString()

        val concurrency = 300

        log.info("Going to create counter {} in database {} times simultaneously", counterName, concurrency)

        val successInsertsValues = CopyOnWriteArrayList<Int>()
        val failedInsertsValues = CopyOnWriteArrayList<Int>()


        try {

            withTimeout(300.seconds) {
                (1..concurrency).map {
                    launch(scenarioDispatcher) {
                        val initialValue = Random.nextInt(100, 1000)
                        try {
                            countersV1RestClient.create(counterName, initialValue)

                            log.info("Successfully created counter #{} with initial value {}", it, initialValue)
                            successInsertsValues.add(initialValue)
                        } catch (e: Exception) {
                            log.info("Failed to create counter #{} with initial value {}", it, initialValue)
                            failedInsertsValues.add(initialValue)
                        }
                    }
                }

            }.joinAll()

            val successfullyCreatedCounter = "${successInsertsValues.firstOrNull() ?: "N/A"}"

            val successfulAttempts = successInsertsValues.size
            val failedAttempts = failedInsertsValues.size

            val actualCounter = countersV1RestClient.get(counterName)

            log.info("Finished executing coroutines with total successful executions: {}", successfulAttempts)
            log.info(
                """\n
                    Finished concurrent creation of items:
                    Counter name:         {}
                    total requests:       {}
                    successful requests:  {}
                    failed requests:      {}
                    actual counter:       {}
                    expected counter:     {}
                """.trimIndent(),
                counterName, concurrency, successfulAttempts, failedAttempts,
                actualCounter.value, successfullyCreatedCounter
            )

        } catch (e: CancellationException) {
            log.error("Scenario didn't finish in 300 seconds timeout", e)
        }

    }


    private suspend fun concurrentIncrementScenarioInternal(
        incrementApiCall: suspend (counterName: String, incrementValue: Int) -> CounterV1ResponseDto
    ) = coroutineScope {


        val counterName = UUID.randomUUID().toString()
        val initialValue = Random.nextInt(100, 1000)

        val newCounter = countersV1RestClient.create(counterName, initialValue)

        log.info("Initial counter {} - {}", counterName, newCounter.value.toString())

        val numAttempts = 100
        val operationsDelaySeconds = 1.seconds
        val concurrency = 300
        val incrementValue = 1
        log.info(
            """\n
            Incrementing counter concurrently:
            times:          {} increment cycles
            concurrency:    {} concurrent requests per cycle
            incrementValue: {}
            estimatedTime:  {} seconds
        """.trimIndent(),
            numAttempts, concurrency, incrementValue, numAttempts * operationsDelaySeconds.toInt(DurationUnit.SECONDS)
        )

        val successResponseCounterValues = ConcurrentHashMap.newKeySet<Int>()
        val failedResponseCoroutineIds = ConcurrentHashMap.newKeySet<Int>()

        (1..numAttempts).forEach { iAttempt ->
            val latchSignal = CompletableDeferred<Unit>()

            val coroutines = (1..concurrency).map { coroutineId ->
                launch(scenarioDispatcher) {
                    try {
                        latchSignal.await()
                        val counterResponse = incrementApiCall(counterName, incrementValue)
                        successResponseCounterValues.add(counterResponse.value)
                    } catch (e: Exception) {
                        failedResponseCoroutineIds.add(iAttempt * 1000 + coroutineId)
                    }
                }
            }

            latchSignal.complete(Unit)

            try {
                withTimeout(300.seconds) {
                    coroutines.joinAll()
                }
            } catch (e: CancellationException) {
                log.error("Scenario didn't finish in 300 seconds timeout", e)
            }

            printCompletionPercent(iAttempt, numAttempts)
            delay(1.seconds)

        }


        val incrementedCounter = countersV1RestClient.get(counterName)

        val expectedIncrement = numAttempts * concurrency * incrementValue
        val expectedValue = initialValue + expectedIncrement
        val uniqueResponseCounterValues = successResponseCounterValues.size
        log.info(
            """\n
            Finished counter increment:
            Counter name:                {}
            Initial value:               {}
            expected increment:          {}
            expected value:              {}
            actual value:                {}
            total increment operations:  {}
            unique response counters:    {}
            """.trimIndent(),
            counterName, initialValue, expectedIncrement, expectedValue, incrementedCounter.value,
            concurrency * numAttempts, uniqueResponseCounterValues
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

