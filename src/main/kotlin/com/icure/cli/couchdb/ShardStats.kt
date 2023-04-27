package com.icure.cli.couchdb

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ShardStats(
    val db_name: String,
    val engine: String,
    val purge_seq: Long,
    val update_seq: Long,
    val sizes: Sizes,
    val props: JsonObject,
    val doc_del_count: Long,
    val doc_count: Long,
    val disk_format_version: Long,
    val committed_update_seq: Long,
    val compacted_seq: Long,
    val compact_running: Boolean,
    val instance_start_time: String,
    val uuid: String,
) {
    @Serializable
    data class Sizes(val file: Long, val external: Long, val active: Long)

}
