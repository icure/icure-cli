@file:OptIn(ExperimentalSerializationApi::class)

package com.icure.cli

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.icure.cli.commands.ListDatabases
import com.icure.cli.commands.ListShards
import com.icure.cli.couchdb.getClient
import kotlinx.serialization.ExperimentalSerializationApi

class ICurecli : CliktCommand() {
    private val credentials by option("-u", "--credentials", help = "Credentials").default("icure:icure")
    private val server by argument(help = "Couchdb server URL").default("http://localhost:5984")

    private val config by findOrSetObject { CliktConfig() }

    override fun run() {
        config.client = getClient(credentials)
        config.server = server
    }
}

fun main(args: Array<String>) = ICurecli().subcommands(
    ListDatabases(),
    ListShards()
).main(args)
