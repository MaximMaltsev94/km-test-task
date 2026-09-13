package edu.mmaltsau.interviews.domain.dictionary.repository

import edu.mmaltsau.interviews.domain.dictionary.model.Counter

interface CounterRepository {
    suspend fun findAll(): List<Counter>
    suspend fun find(name: String): Counter?
    suspend fun insert(name: String, value: Int): Counter
    suspend fun update(name: String, value: Int): Counter?
    suspend fun delete(name: String): Int
    suspend fun increaseValue(name: String, incValue: Int): Counter?
}