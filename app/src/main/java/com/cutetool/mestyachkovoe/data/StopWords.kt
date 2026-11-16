package com.cutetool.mestyachkovoe.data

import java.util.Locale

object StopWords {
    private val common = listOf(
        "гороскоп", "астролог", "таро", "нумеролог", "ретроградный меркурий",
        "сонник", "примета", "эзотерик", "маг", "ритуал"
    )

    private val neighbors: Map<Region, List<String>> = mapOf(
        Region.CHELYABINSK to listOf("свердлов", "екатеринбург", "тюмень", "курган", "уфа", "башкир", "оренбург", "пермь", "хмао", "янао"),
        Region.SVERDLOVSK to listOf("челябин", "курган", "тюмень", "пермь", "уфа", "башкир", "оренбург", "хмао", "янао"),
        Region.KURGAN to listOf("челябин", "свердлов", "екатеринбург", "тюмень", "уфа", "башкир", "оренбург", "пермь"),
        Region.TYUMEN to listOf("курган", "челябин", "свердлов", "екатеринбург", "пермь", "уфа", "башкир", "оренбург"),
        Region.KHMAO to listOf("янао", "тюмень", "челябин", "свердлов", "екатеринбург", "курган", "пермь", "уфа", "оренбург"),
        Region.YANAO to listOf("хмао", "тюмень", "челябин", "свердлов", "екатеринбург", "курган", "пермь", "уфа", "оренбург"),
        Region.PERM to listOf("челябин", "свердлов", "екатеринбург", "курган", "тюмень", "уфа", "башкир", "оренбург", "киров", "удмурт"),
        Region.BASHKORTOSTAN to listOf("челябин", "оренбург", "пермь", "свердлов", "екатеринбург", "тюмень", "курган"),
        Region.ORENBURG to listOf("челябин", "уфа", "башкир", "самар", "свердлов", "екатеринбург", "тюмень", "курган")
    )

    private val svoRoots = listOf(
        "сво", "спецоперац", "полномасштабн", "украин", "всу", "мобилизац",
        "фронт", "обстрел", "беспилотник", "бпла", "дрон", "военк", "контрактник",
        "штурм", "окоп", "боев", "минобор"
    )

    private val adultRoots = listOf(
        "эротик", "эротическ", "белье", "бельё", "трус", "лифчик", "бюстгальтер",
        "интим", "обнажен", "обнажён", "голая", "голые", "голый", "порно", "секс",
        "стриптиз", "18+", "онлифанс", "onlyfans"
    )

    fun blocksRegion(region: Region, text: String): Boolean {
        if (region == Region.ALL || text.isBlank()) return false
        val lower = text.lowercase(Locale.ROOT)
        if (common.any { lower.contains(it) }) return true
        return neighbors[region].orEmpty().any { lower.contains(it) }
    }

    fun blocksSvoPolitics(text: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.lowercase(Locale.ROOT)
        return svoRoots.any { lower.contains(it) }
    }

    fun blocksAdult(text: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.lowercase(Locale.ROOT)
        return adultRoots.any { lower.contains(it) }
    }
}