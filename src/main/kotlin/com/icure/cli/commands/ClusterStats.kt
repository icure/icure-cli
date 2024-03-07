package com.icure.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.path
import com.icure.cli.CliktConfig
import com.icure.cli.couchdb.DbStats
import com.icure.cli.utils.getGroupHierarchy
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.FileWriter

@ExperimentalSerializationApi
class ClusterStats : CliktCommand(help = "Creates a report of all the group databases in a cluster, divided by group, and saves it to a file.") {
    private val config by requireObject<CliktConfig>()
    private val format by option(
        help = "The output format. The .tsv format will contain only a summary of the data (group, root supergroup and size)"
    ).switch(
        "--json" to ExportFormat.JSON,
        "--tsv" to ExportFormat.TSV
    ).default(ExportFormat.JSON)
    private val outputFile by option("-o", "--output", help = "The output file where to save the result").path().required()

    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    override fun run() {
        runBlocking {
            val dbs = config.client.get("${config.server}/_all_dbs").body<List<String>>()
                .filter {
                    Regex("icure-[a-zA-Z0-9\\-]+-(base|healthdata|patient)").matches(it)
                }.map {
                    config.client.get("${config.server}/$it").body<DbStats>()
                }.groupBy {
                    it.db_name.replace("icure-", "").replace(Regex("-(base|healthdata|patient)"), "")
                }.map {
                    it.key to DbGroupInfo(
                        stats = it.value,
                        groupHierarchy = config.getGroupHierarchy(it.key)
                    )
                }.toMap()
            when(format) {
                ExportFormat.JSON -> FileWriter(outputFile.toFile()).use { fw ->
                    fw.write(json.encodeToString(dbs))
                }
                ExportFormat.TSV -> FileWriter(outputFile.toFile()).use { fw ->
                    fw.write("GROUP_NAME\tROOT_SUPERGROUP\tBYTE_SIZE\n")
                    dbs.forEach {
                        fw.write(
                            buildString {
                                append("${it.key}\t")
                                append("${it.value.groupHierarchy.first()}\t")
                                val totalSize = it.value.stats.sumOf { it.sizes.active }
                                append("${totalSize}\n")
                            }
                        )
                    }
                }
            }

        }
    }

    private enum class ExportFormat { JSON, TSV }

    @Serializable
    private data class DbGroupInfo(
        val stats: List<DbStats>,
        val groupHierarchy: List<String>
    )

}

