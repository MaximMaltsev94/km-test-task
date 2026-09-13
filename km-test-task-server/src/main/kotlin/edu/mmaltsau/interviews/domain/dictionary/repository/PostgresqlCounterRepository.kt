package edu.mmaltsau.interviews.domain.dictionary.repository

import edu.mmaltsau.interviews.domain.dictionary.model.Counter
import edu.mmaltsau.interviews.server.exceptions.RepositoryInsertConflictException
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.updateReturning

class PostgresqlCounterRepository(private val db: R2dbcDatabase) : CounterRepository {

    object Counters : Table("counters") {
        val name = varchar("name", 50).uniqueIndex()
        val value = integer("value").default(0)

        override val primaryKey = PrimaryKey(name)
    }


    override suspend fun findAll(): List<Counter> = suspendTransaction(db) {
        Counters.selectAll().mapNotNull {
            Counter(it[Counters.name], it[Counters.value])
        }.toList()
    }

    override suspend fun find(name: String): Counter? = suspendTransaction(db) {
        Counters.selectAll()
            .where { Counters.name eq name }
            .singleOrNull()
            ?.toCounter()
    }

    override suspend fun insert(
        name: String,
        value: Int
    ): Counter {
        try {
            return suspendTransaction(db) {

                val newRecord = Counters.insert {
                    it[Counters.name] = name
                    it[Counters.value] = value
                }
                Counter(newRecord[Counters.name], newRecord[Counters.value])

            }

        } catch (sqlException: Exception) {
            throw RepositoryInsertConflictException("Could not insert counter: $name", sqlException)
        }
    }

    override suspend fun update(
        name: String,
        value: Int
    ): Counter? = suspendTransaction(db) {

        Counters.updateReturning(where = { Counters.name eq name }) {
            it[Counters.value] = value
        }
            .singleOrNull()
            ?.toCounter()
    }

    override suspend fun delete(name: String) = suspendTransaction(db) {
        Counters.deleteWhere { Counters.name eq name }
    }

    override suspend fun increaseValue(name: String, incValue: Int): Counter? = suspendTransaction(db) {
        Counters.updateReturning(where = { Counters.name eq name }) {
            it[Counters.value] = Counters.value + incValue
        }
            .singleOrNull()
            ?.toCounter()

    }

    private fun ResultRow.toCounter(): Counter = Counter(this[Counters.name], this[Counters.value])

}

