package com.icure.cli.utils

import com.icure.cli.CliktConfig
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable


suspend fun CliktConfig.getGroupHierarchy(groupId: String, hierarchy: List<String> = emptyList()): List<String> =
    try {
        val groupAndSuperGroup = this.client.get(
            "${this.server}/icure-__-config/$groupId"
        ).body<GroupAndSuperGroup>()
        if (groupAndSuperGroup.superGroup == null) listOf(groupId) + hierarchy
        else this.getGroupHierarchy(groupAndSuperGroup.superGroup,listOf(groupId) + hierarchy )
    } catch (e: Exception) {
        listOf(groupId) + hierarchy
    }


@Serializable
data class GroupAndSuperGroup(
    val _id: String,
    val superGroup: String? = null
)