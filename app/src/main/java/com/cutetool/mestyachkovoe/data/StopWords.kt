package com.cutetool.mestyachkovoe.data

import java.util.Locale

object StopWords {
    private val common = listOf(
        "спецоперац",
        "кремл",
        "госдум",
        "совфед",
        "совет федерации",
        "правительство росси",
        "миноборон",
        "москв",
        "путин"
    )

    private val neighbors: Map<Region, List<String>> = mapOf(
        Region.CHELYABINSK to listOf(
            "екатеринбург", "свердловск", "тюмен", "курган", "уфа", "башкир",
            "оренбург", "перм", "хмао", "янао"
        ),
        Region.SVERDLOVSK to listOf(
            "челябинск", "курган", "тюмен", "перм", "уфа", "башкир",
            "оренбург", "хмао", "янао"
        ),
        Region.KURGAN to listOf(
            "челябинск", "екатеринбург", "свердловск", "тюмен", "уфа",
            "башкир", "оренбург", "перм"
        ),
        Region.TYUMEN to listOf(
            "курган", "екатеринбург", "свердловск", "челябинск", "перм",
            "уфа", "башкир", "оренбург"
        ),
        Region.KHMAO to listOf(
            "янао", "тюмен", "екатеринбург", "свердловск", "челябинск",
            "курган", "перм", "уфа", "оренбург"
        ),
        Region.YANAO to listOf(
            "хмао", "тюмен", "екатеринбург", "свердловск", "челябинск",
            "курган", "перм", "уфа", "оренбург"
        ),
        Region.PERM to listOf(
            "екатеринбург", "свердловск", "челябинск", "курган", "тюмен",
            "уфа", "башкир", "оренбург", "ижевск", "киров"
        ),
        Region.BASHKORTOSTAN to listOf(
            "челябинск", "оренбург", "перм", "екатеринбург", "свердловск",
            "тюмен", "курган"
        ),
        Region.ORENBURG to listOf(
            "челябинск", "уфа", "башкир", "самар", "екатеринбург",
            "свердловск", "тюмен", "курган"
        )
    )

    private val svoRegex = Regex("(?<![а-яёa-z])сво(?![а-яёa-z])", RegexOption.IGNORE_CASE)

    fun blocks(region: Region, text: String): Boolean {
        if (region == Region.ALL || text.isBlank()) return false
        val lower = text.lowercase(Locale.ROOT)
        if (svoRegex.containsMatchIn(lower)) return true
        if (common.any { lower.contains(it) }) return true
        return neighbors[region].orEmpty().any { lower.contains(it) }
    }
}
