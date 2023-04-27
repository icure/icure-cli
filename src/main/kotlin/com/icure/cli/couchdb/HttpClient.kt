package com.icure.cli.couchdb

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import kotlinx.serialization.json.Json

fun getClient(credentials: String) = HttpClient {
    defaultRequest {
        headers["Authorization"] =
            "Basic ${credentials.toByteArray(Charsets.UTF_8).encodeBase64()}"
    }

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 60000
    }
}