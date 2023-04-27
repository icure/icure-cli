package com.icure.cli.couchdb

import kotlinx.serialization.Serializable

@Serializable
data class DbInfo(
    val _id: String,
    val _rev: String,
    val shard_suffix: List<Int>,
    val changelog: List<List<String>>,
    val by_node: Map<String, List<String>>,
    val by_range: Map<String, List<String>>,
    val props: Map<String, String>
) {
    fun checkOneShardedAndBasicChangelog() {
        check(by_range.size == 1) { "Too many shards" }
        check(changelog.all { it[0] == "add" })
    }

    fun keepOnlyReplication(toKeep: String): DbInfo {
        val changelogAddEntry = if (changelog.any { it[2] == toKeep }) {
            emptyList()
        } else {
            listOf(changelog.first().dropLast(1) + listOf(toKeep))
        }
        val changelogRemoveEntries = changelog.flatMap {
            if (it[2] == toKeep) {
                emptyList()
            } else {
                listOf(listOf("remove") + it.drop(1))
            }
        }
        val shardName = changelog.first()[1]
        return copy(
            changelog = changelog + changelogAddEntry + changelogRemoveEntries,
            by_node = mapOf(toKeep to listOf(shardName)),
            by_range = mapOf(shardName to listOf(toKeep))
        )
    }
}