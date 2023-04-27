package com.icure.cli.couchdb

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class DbStats(
    val db_name: String,
    val purge_seq: String,
    val update_seq: String,
    val sizes: Sizes,
    val props: JsonObject,
    val doc_del_count: Long,
    val doc_count: Long,
    val disk_format_version: Long,
    val compact_running: Boolean,
    val cluster: ClusterStats,
    val instance_start_time: String
) {
    @Serializable
    data class Sizes(val file: Long, val external: Long, val active: Long)

    @Serializable
    data class ClusterStats(val q: Long, val n: Long, val w: Long, val r: Long)
}
