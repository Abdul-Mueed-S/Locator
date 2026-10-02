package com.evu.locator.utils

fun String.cleanTrim(): String =
    this.trim().replace(Regex("\\s+"), " ")

fun String.isBlankAfterClean(): Boolean =
    this.cleanTrim().isEmpty()