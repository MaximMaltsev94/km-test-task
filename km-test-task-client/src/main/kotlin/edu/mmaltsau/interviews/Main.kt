package edu.mmaltsau.interviews

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Scanner

suspend fun main() {
    val log = LoggerFactory.getLogger("main")

    val client = HttpClient(CIO) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        install(Logging)
    }

    val appHost = System.getenv("APP_HOST") ?: "http://localhost:44555"
    val countersV1RestClient = CountersV1RestClient(appHost, client)
    val appScenarios = ClientAppScenarios(countersV1RestClient)

    runMenu(appScenarios, log)

    client.close()
}


suspend fun runMenu(
    appScenarios: ClientAppScenarios,
    log: Logger
) {
    val scanner = Scanner(System.`in`)
    log.info("KM test task client application. Type 'help' to see available commands or 'quit' to exit.")

    while (true) {
        try {

            printHelp(log)
            log.info("\nEnter command > ")
            val input = scanner.nextLine().trim().lowercase()

            when (input) {
                "quit", "q" -> {
                    log.info("Exiting application...")
                    break
                }

                "help", "h" -> {
                    printHelp(log)
                }

                "1" -> appScenarios.printAllCounters()
                "2" -> appScenarios.concurrentIncrementSafe()
                "3" -> appScenarios.concurrentIncrementUnsafe()
                "4" -> appScenarios.concurrentInsert()
                "" -> continue // Ignore empty inputs
                else -> {
                    log.warn("Unknown command: '$input'. Type 'help' for a list of valid choices.")
                }
            }
        } catch (e: Exception) {
            log.error("Exception occurred while running app scenario.", e)
        }
    }
}

fun printHelp(log: Logger) {
    log.info(
        """\n
    === Available Commands ===
    1    - Print all counters
    2    - Concurrent Increment scenario - correct concurrency control
    3    - Concurrent Increment scenario - unsafe update operation
    4    - Concurrent Insert
    help - Display this help menu (or 'h')
    quit - Exit the application (or 'q')
    ==========================
    """.trimIndent()
    )
}


