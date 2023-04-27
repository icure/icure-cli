package com.icure.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.icure.cli.CliktConfig
import com.icure.cli.byteSize
import com.icure.cli.couchdb.ShardStats
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

class ListShards : CliktCommand(help = "List databases") {
    private val config by requireObject<CliktConfig>()
    private val minSize by option("-m", "--min-size", help = "Minimum size").byteSize().default(0)
    private val minCompactionGain by option(
        "-c",
        "--min-compaction-gain",
        help = "Minimum size that can be reclaimed by compacting"
    ).byteSize().default(0)

    override fun run() {
        runBlocking {
            config.client.get("${config.server}/_node/_local/_all_dbs").body<List<String>>().forEach {
                when {
                    minSize > 0 -> it.executeIfFileSizeBiggerThan(minSize) { echo(it) }
                    minCompactionGain > 0 -> it.executeIfCompactionGainBiggerThan(minCompactionGain) { echo(it) }
                    else -> echo(it)
                }
            }
        }
    }

    private suspend fun String.executeIfFileSizeBiggerThan(minSize: Long, exec: () -> Unit) {
        try {
            val stats = config.client.get("${config.server}/_node/_local/${encodeURLPathPart()}").body<ShardStats>()
            if (stats.sizes.file > minSize) exec()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private suspend fun String.executeIfCompactionGainBiggerThan(minSize: Long, exec: () -> Unit) {
        try {
            val stats = config.client.get("${config.server}/_node/_local/${encodeURLPathPart()}").body<ShardStats>()
             if (stats.sizes.file - stats.sizes.active > minSize) exec()
        } catch (e: Exception) {
            // Ignore
        }
    }
}