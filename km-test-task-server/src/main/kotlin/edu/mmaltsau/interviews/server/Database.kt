package edu.mmaltsau.interviews.server

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import java.time.Duration

fun Application.configureDatabase() {

    val pgConnectionString = environment.config.property("postgres.connectionString").getString()
    val pgHost = environment.config.property("postgres.host").getString()
    val pgPort = environment.config.property("postgres.port").getString().toInt()
    val dbName = environment.config.property("postgres.dbName").getString()
    val pgUser = environment.config.property("postgres.user").getString()
    val pgPassword = environment.config.property("postgres.password").getString()
    Class.forName("org.postgresql.Driver")

    val connectionFactory = ConnectionFactories.get(
        ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, "postgresql")
            .option(ConnectionFactoryOptions.HOST, pgHost)
            .option(ConnectionFactoryOptions.PORT, pgPort)
            .option(ConnectionFactoryOptions.USER, pgUser)
            .option(ConnectionFactoryOptions.PASSWORD, pgPassword)
            .option(ConnectionFactoryOptions.DATABASE, dbName)
            .build()
    )

    val poolConfig = ConnectionPoolConfiguration.builder(connectionFactory)
        .maxIdleTime(Duration.ofMillis(5000))
        .maxSize(100)
        .build()

    val connectionPool = ConnectionPool(poolConfig);

    dependencies {
        provide {
            R2dbcDatabase.connect(
                connectionFactory = connectionPool,
                databaseConfig = R2dbcDatabaseConfig {
                    explicitDialect = PostgreSQLDialect()
                }
            )
        }
    }
}