package com.icure.cli.utils

import kotlin.math.ceil
import kotlin.math.floor

tailrec fun humanReadableSize(size: Size): Size =
    if (size.value >= 1000 && size.unit.next != null)
        humanReadableSize(size.copy(
            value = floor(size.value / 10) / 100,
            unit = size.unit.next
        ))
    else size

enum class SizeUnit(val next: SizeUnit?, val stringValue: String) {
    TERABYTE(next = null, stringValue = "Tb"),
    GIGABYTE(next = TERABYTE, stringValue = "Gb"),
    MEGABYTE(next = GIGABYTE, stringValue = "Mb"),
    KILOBYTE(next = MEGABYTE, stringValue = "Kb"),
    BYTE(next = KILOBYTE, stringValue = "b")
}

data class Size(val value: Double, val unit: SizeUnit = SizeUnit.BYTE)