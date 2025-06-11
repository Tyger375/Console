package com.takanenstudios.console_emulator

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation

val httpClient
    get() = HttpClient(CIO) {
        install(ContentNegotiation)
    }

const val address = "http://localhost:8080"