package com.icure.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.icure.cli.CliktConfig
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking

class ListDatabases : CliktCommand(help = "List databases") {
    private val config by requireObject<CliktConfig>()

    override fun run() {
        runBlocking {
            config.client.get("${config.server}/_all_dbs").body<List<String>>().forEach {
                echo(it)
            }
        }
    }
}