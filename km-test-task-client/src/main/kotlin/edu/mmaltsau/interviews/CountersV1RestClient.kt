package edu.mmaltsau.interviews

import edu.mmaltsau.interviews.dto.CounterCreateRequestDto
import edu.mmaltsau.interviews.dto.CounterIncrementRequestDto
import edu.mmaltsau.interviews.dto.CounterV1ResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.slf4j.LoggerFactory

class CountersV1RestClient(
    private val baseHost: String,
    private val httpClient: HttpClient) {

    companion object {
        private const val BASE_API = "/api/v1/dictionaries/default/counters"
        private val log = LoggerFactory.getLogger(CountersV1RestClient::class.java)
    }

    suspend fun getAll(): List<CounterV1ResponseDto> {
        return httpClient.get("$baseHost$BASE_API").body()
    }

    suspend fun get(name: String): CounterV1ResponseDto {
        return httpClient.get("$baseHost$BASE_API/$name").body()
    }

    suspend fun create(name: String, initialValue: Int): CounterV1ResponseDto {
        return httpClient.put("$baseHost$BASE_API/$name") {
            contentType(ContentType.Application.Json)
            setBody(CounterCreateRequestDto(initialValue))
        }.body()
    }

    suspend fun increment(name: String, incValue: Int){
        httpClient.post("$baseHost$BASE_API/$name/increments") {
            contentType(ContentType.Application.Json)
            setBody(CounterIncrementRequestDto(incValue))
        }
    }

    suspend fun incrementUnsafe(name: String, incValue: Int){
        httpClient.post("$baseHost$BASE_API/$name/increments/unsafe") {
            contentType(ContentType.Application.Json)
            setBody(CounterIncrementRequestDto(incValue))
        }
    }

}