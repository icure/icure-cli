package com.icure.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.icure.cli.CliktConfig
import com.icure.cli.couchdb.DbStats
import com.icure.cli.utils.Size
import com.icure.cli.utils.getGroupHierarchy
import com.icure.cli.utils.humanReadableSize
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking

class GroupInfo : CliktCommand(help = "Show the information about a group") {
    private val config by requireObject<CliktConfig>()
    private val group by option("-g", "--group", help = "The group to show the information for").required()
    private val humanReadable by option("-H", help = "Show size values in Kb, Mb and Gb").flag()

    override fun run() {
        runBlocking {
            val dbs = config.client.get("${config.server}/_all_dbs").body<List<String>>().filter {
                it.matches(Regex("icure-$group-.+"))
            }.map {
                config.client.get("${config.server}/$it").body<DbStats>()
            }
            val hierarchy = config.getGroupHierarchy(group)
            val output = if(dbs.isNotEmpty())
                buildString {
                    append("Databases for the group $group:\n")
                    dbs.forEach { db ->
                        append("\t${db.db_name}:\n")
                        append("\t\tdocuments: ${db.doc_count}\n")
                        if (humanReadable)
                            append("\t\tsize: ${humanReadableSize(Size(db.sizes.active.toDouble())).let { "${it.value} ${it.unit.stringValue}" }}\n")
                        else
                            append("\t\tsize: ${db.sizes.active}\n")
                        append("\n")
                    }
                    append("Total documents: ${dbs.sumOf { it.doc_count }}\n")
                    val totalSize = dbs.sumOf { it.sizes.active.toDouble() }
                    if (humanReadable)
                        append("size: ${humanReadableSize(Size(totalSize)).let { "${it.value} ${it.unit.stringValue}" }}\n")
                    else
                        append("size: $totalSize b\n")
                    append("Group hierarchy:\n")
                    append("${hierarchy.first()}\n")
                    hierarchy.drop(1).fold(0) { tabs, g ->
                        append("${"\t".repeat(tabs)}|\n")
                        append("${"\t".repeat(tabs)}*-$g\n")
                        tabs + 1
                    }
                }
                else "No databases found for group $group"
            echo(output)
        }
    }

}