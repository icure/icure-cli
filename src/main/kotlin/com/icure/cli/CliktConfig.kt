package com.icure.cli

import io.ktor.client.*

class CliktConfig {
    private var _client: HttpClient? = null
    private var _server: String? = null

    var client: HttpClient
        get() = _client!!
        set(value) {
            _client = value
        }

    var server: String
        get() = _server!!
        set(value) {
            _server = value
        }
}