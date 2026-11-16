package com.cutetool.mestyachkovoe.data

enum class Region(val title: String) {
    ALL("Все"),
    CHELYABINSK("Челябинская"),
    SVERDLOVSK("Свердловская"),
    KURGAN("Курганская"),
    TYUMEN("Тюменская"),
    KHMAO("ХМАО"),
    YANAO("ЯНАО"),
    PERM("Пермский край"),
    BASHKORTOSTAN("Башкирия"),
    ORENBURG("Оренбургская")
}

enum class ThemeMode(val title: String) { MONO("Ч/Б"), MONET("Monet") }

enum class NotifyFrequency(val title: String, val intervalMinutes: Long) {
    INSTANT("Сразу (каждые 15 минут)", 15),
    DIGEST_3H("Дайджест раз в 3 часа", 180),
    DAILY("Раз в день", 1440)
}

enum class NotifyImportance(val title: String) {
    EMERGENCY("Только ЧС"),
    MAIN("Главные"),
    ALL("Все")
}

enum class AppLabel(val title: String, val baseAlias: String) {
    MESTYACHKOVOE("Местячковое", "Mestyachkovoe"),
    URALSKOE("Уральское", "Uralskoe"),
    NOVOSTI("Новости", "Novosti")
}

enum class LauncherIcon(val title: String) {
    NEW("Новая"),
    OLD("Предыдущая")
}

data class NewsItem(
    val id: String,
    val title: String,
    val link: String,
    val imageUrl: String?,
    val source: String,
    val published: Long,
    val region: Region,
    val content: String = ""
)

data class FeedSource(
    val name: String,
    val url: String,
    val region: Region
)

object Sources {
    val all: List<FeedSource> = listOf(
        FeedSource("Накануне.RU", "https://www.nakanune.ru/rss/", Region.ALL),
        FeedSource("ЕАН", "https://eanews.ru/rss", Region.ALL),
        FeedSource("Правда УрФО", "https://pravdaurfo.ru/rss", Region.ALL),
        FeedSource("URA.RU Челябинск", "https://ura.news/rss/chel", Region.CHELYABINSK),
        FeedSource("74.ru", "https://74.ru/rss/", Region.CHELYABINSK),
        FeedSource("1obl.ru", "https://www.1obl.ru/rss/", Region.CHELYABINSK),
        FeedSource("Доступ", "https://dostup1.ru/rss/", Region.CHELYABINSK),
        FeedSource("КП Челябинск", "https://chel.kp.ru/rss/all.xml", Region.CHELYABINSK),
        FeedSource("Курс дела", "https://kursdela.biz/rss/", Region.CHELYABINSK),
        FeedSource("URA.RU Свердловская", "https://ura.news/rss/svrd", Region.SVERDLOVSK),
        FeedSource("E1.RU", "https://www.e1.ru/rss/", Region.SVERDLOVSK),
        FeedSource("66.ru", "https://66.ru/rss/", Region.SVERDLOVSK),
        FeedSource("Областная газета", "https://oblgazeta.ru/rss/", Region.SVERDLOVSK),
        FeedSource("URA.RU Курган", "https://ura.news/rss/kurgan", Region.KURGAN),
        FeedSource("45.ru", "https://45.ru/rss/", Region.KURGAN),
        FeedSource("oblast45", "https://oblast45.ru/rss/", Region.KURGAN),
        FeedSource("NM45", "https://nm45.ru/rss/", Region.KURGAN),
        FeedSource("URA.RU Тюмень", "https://ura.news/rss/tumen", Region.TYUMEN),
        FeedSource("72.ru", "https://72.ru/rss/", Region.TYUMEN),
        FeedSource("Вслух.ру", "https://vsluh.ru/rss/", Region.TYUMEN),
        FeedSource("URA.RU ХМАО", "https://ura.news/rss/hmao", Region.KHMAO),
        FeedSource("Муксун.fm", "https://muksun.fm/rss/", Region.KHMAO),
        FeedSource("URA.RU ЯНАО", "https://ura.news/rss/yamal", Region.YANAO),
        FeedSource("89.ru", "https://89.ru/rss/", Region.YANAO),
        FeedSource("URA.RU Пермь", "https://ura.news/rss/perm", Region.PERM),
        FeedSource("59.ru", "https://59.ru/rss/", Region.PERM),
        FeedSource("ProPerm", "https://properm.ru/rss/", Region.PERM),
        FeedSource("Ufa1.ru", "https://ufa1.ru/rss/", Region.BASHKORTOSTAN),
        FeedSource("1743.ru", "https://1743.ru/rss/", Region.ORENBURG),
    )
}
