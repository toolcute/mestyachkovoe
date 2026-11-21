package com.cutetool.mestyachkovoe.data

import java.net.URLEncoder

enum class DistrictStatus(val label: String) {
    AVAILABLE(""),
    TESTING("тестируется"),
    SOON("скоро")
}

enum class FederalDistrict(
    val title: String,
    val status: DistrictStatus = DistrictStatus.AVAILABLE,
    val selectable: Boolean = true
) {
    URAL("Урал"),
    CENTRAL("Центральная Россия"),
    VOLGA("Приволжье (ПФО)"),
    SIBERIA("Сибирь"),
    MOSCOW_OBLAST("Московская область", DistrictStatus.TESTING, true),
    SOUTH("ЮФО", DistrictStatus.TESTING, false),
    NORTH_CAUCASUS("Северный Кавказ", DistrictStatus.TESTING, false),
    NORTHWEST("СЗФО", DistrictStatus.SOON, false),
    FAR_EAST("Дальний Восток", DistrictStatus.SOON, false);

    val regions: List<Region>
        get() = Region.entries.filter { it != Region.ALL && it.district == this }
}

enum class Region(val title: String) {
    ALL("Все"),

    // Урал
    CHELYABINSK("Челябинская"),
    SVERDLOVSK("Свердловская"),
    KURGAN("Курганская"),
    TYUMEN("Тюменская"),
    KHMAO("ХМАО"),
    YANAO("ЯНАО"),
    PERM("Пермский край"),
    BASHKORTOSTAN("Башкирия"),
    ORENBURG("Оренбургская"),

    // Центральная Россия (без Москвы и Московской области)
    BELGOROD("Белгородская"),
    BRYANSK("Брянская"),
    VLADIMIR("Владимирская"),
    VORONEZH("Воронежская"),
    IVANOVO("Ивановская"),
    KALUGA("Калужская"),
    KOSTROMA("Костромская"),
    KURSK("Курская"),
    LIPETSK("Липецкая"),
    OREL("Орловская"),
    RYAZAN("Рязанская"),
    SMOLENSK("Смоленская"),
    TAMBOV("Тамбовская"),
    TVER("Тверская"),
    TULA("Тульская"),
    YAROSLAVL("Ярославская"),

    // Московская область
    MOSCOW_OBLAST("Московская область"),

    // Приволжье
    KIROV("Кировская"),
    MARI_EL("Марий Эл"),
    MORDOVIA("Мордовия"),
    NIZHNY_NOVGOROD("Нижегородская"),
    PENZA("Пензенская"),
    SAMARA("Самарская"),
    SARATOV("Саратовская"),
    TATARSTAN("Татарстан"),
    UDMURTIA("Удмуртия"),
    ULYANOVSK("Ульяновская"),
    CHUVASHIA("Чувашия"),

    // Сибирь
    ALTAI("Алтайский край"),
    BURYATIA("Бурятия"),
    ZABAIKALYE("Забайкальский край"),
    IRKUTSK("Иркутская"),
    KEMEROVO("Кемеровская"),
    KRASNOYARSK("Красноярский край"),
    NOVOSIBIRSK("Новосибирская"),
    OMSK("Омская"),
    TOMSK("Томская"),
    TYVA("Тыва"),
    KHAKASSIA("Хакасия")
}

val Region.district: FederalDistrict
    get() = when (this) {
        Region.ALL,
        Region.CHELYABINSK,
        Region.SVERDLOVSK,
        Region.KURGAN,
        Region.TYUMEN,
        Region.KHMAO,
        Region.YANAO,
        Region.PERM,
        Region.BASHKORTOSTAN,
        Region.ORENBURG -> FederalDistrict.URAL

        Region.BELGOROD,
        Region.BRYANSK,
        Region.VLADIMIR,
        Region.VORONEZH,
        Region.IVANOVO,
        Region.KALUGA,
        Region.KOSTROMA,
        Region.KURSK,
        Region.LIPETSK,
        Region.OREL,
        Region.RYAZAN,
        Region.SMOLENSK,
        Region.TAMBOV,
        Region.TVER,
        Region.TULA,
        Region.YAROSLAVL -> FederalDistrict.CENTRAL

        Region.MOSCOW_OBLAST -> FederalDistrict.MOSCOW_OBLAST

        Region.KIROV,
        Region.MARI_EL,
        Region.MORDOVIA,
        Region.NIZHNY_NOVGOROD,
        Region.PENZA,
        Region.SAMARA,
        Region.SARATOV,
        Region.TATARSTAN,
        Region.UDMURTIA,
        Region.ULYANOVSK,
        Region.CHUVASHIA -> FederalDistrict.VOLGA

        Region.ALTAI,
        Region.BURYATIA,
        Region.ZABAIKALYE,
        Region.IRKUTSK,
        Region.KEMEROVO,
        Region.KRASNOYARSK,
        Region.NOVOSIBIRSK,
        Region.OMSK,
        Region.TOMSK,
        Region.TYVA,
        Region.KHAKASSIA -> FederalDistrict.SIBERIA
    }

enum class ThemeMode(val title: String) { MONET("Monet"), MONO("Ч/Б (Тёмная)"), LIGHT("Светлая") }

enum class NotifyFrequency(val title: String, val intervalMinutes: Long, val importantOnly: Boolean = false) {
    INSTANT("Сразу (примерно 15 минут)", 15),
    DIGEST_3H("Дайджест раз в 3 часа", 180),
    IMPORTANT_3X("3 важных в день", 480, importantOnly = true),
    IMPORTANT_4X("4 важных в день", 360, importantOnly = true),
    DAILY("Раз в день", 1440)
}

enum class NotifyImportance(val title: String) {
    EMERGENCY("Только ЧС"),
    MAIN("Главное"),
    ALL("Все")
}

enum class CacheAutoClean(val title: String, val intervalHours: Long?) {
    OFF("Выключена", null),
    DAILY("Каждый день", 24),
    WEEKLY("Раз в неделю", 24 * 7),
    MONTHLY("Раз в месяц", 24 * 30)
}

enum class AppLabel(val title: String, val baseAlias: String) {
    MESTYACHKOVOE("Местячковое", "Mestyachkovoe"),
    URALSKOE("Уральское", "Uralskoe"),
    NOVOSTI("Новости", "Novosti")
}

enum class LauncherIcon(val title: String) {
    NEW("Новая"),
    OLD("Прежняя")
}

data class NewsItem(
    val id: String,
    val title: String,
    val link: String,
    val imageUrl: String?,
    val source: String,
    val published: Long,
    val region: Region,
    val content: String = "",
    val sourceVerified: Boolean = false
)

data class FeedSource(
    val name: String,
    val url: String,
    val region: Region,
    val district: FederalDistrict = FederalDistrict.URAL,
    val verified: Boolean = false
)

object Sources {
    private fun googleNews(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return "https://news.google.com/rss/search?q=$encoded&hl=ru&gl=RU&ceid=RU:ru"
    }

    val all: List<FeedSource> = listOf(
        // Урал
        FeedSource("Накануне.RU", "https://www.nakanune.ru/rss/", Region.ALL),
        FeedSource("ЕАН", "https://eanews.ru/rss", Region.ALL),
        FeedSource("Правда УрФО", "https://pravdaurfo.ru/rss", Region.ALL),
        FeedSource("URA.RU Челябинск", "https://ura.news/rss/chel", Region.CHELYABINSK),
        FeedSource("74.ru", "https://74.ru/rss/", Region.CHELYABINSK),
        FeedSource("1obl.ru", "https://www.1obl.ru/rss/", Region.CHELYABINSK),
        FeedSource("Доступ", "https://dostup1.ru/rss/", Region.CHELYABINSK),
        FeedSource("КП Челябинск", "https://chel.kp.ru/rss/all.xml", Region.CHELYABINSK),
        FeedSource("Курс дела", "https://kursdela.biz/rss/", Region.CHELYABINSK),
        FeedSource(
            "Местечко",
            "https://toolcute.github.io/Mestechko/rss.xml",
            Region.CHELYABINSK,
            FederalDistrict.URAL,
            verified = true
        ),
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

        // Центральная Россия (без Москвы и Московской области)
        FeedSource("АиФ Тверь", "https://tver.aif.ru/rss/all.php", Region.TVER, FederalDistrict.CENTRAL),
        FeedSource("TverLife", "https://tverlife.ru/export/rss", Region.TVER, FederalDistrict.CENTRAL),
        FeedSource("Google Новости: Тверская область", googleNews("Тверская область новости"), Region.TVER, FederalDistrict.CENTRAL),
        FeedSource("RZN.info Владимир", "https://rss.rzn.info/rss/news/vladimir.xml", Region.VLADIMIR, FederalDistrict.CENTRAL),
        FeedSource("Зебра ТВ", "https://zebra-tv.ru/rss/", Region.VLADIMIR, FederalDistrict.CENTRAL),
        FeedSource("Pro Город Владимир", "https://progorod33.ru/rss.xml", Region.VLADIMIR, FederalDistrict.CENTRAL),
        FeedSource("АиФ Иваново", "https://ivanovo.aif.ru/rss/all.php", Region.IVANOVO, FederalDistrict.CENTRAL),
        FeedSource("Ивановская газета", "https://ivgazeta.ru/rss", Region.IVANOVO, FederalDistrict.CENTRAL),
        FeedSource("Google Новости: Ивановская область", googleNews("Ивановская область новости"), Region.IVANOVO, FederalDistrict.CENTRAL),
        FeedSource("АиФ Калуга", "https://kaluga.aif.ru/rss/all.php", Region.KALUGA, FederalDistrict.CENTRAL),
        FeedSource("Калужская неделя", "https://nedelya40.ru/feed/", Region.KALUGA, FederalDistrict.CENTRAL),
        FeedSource("Знамя", "https://znamkaluga.ru/rss", Region.KALUGA, FederalDistrict.CENTRAL),
        FeedSource("Обнинск", "https://obninsk.name/rss.xml", Region.KALUGA, FederalDistrict.CENTRAL),
        FeedSource("RZN.info Калуга", "https://rss.rzn.info/rss/news/kaluga.xml", Region.KALUGA, FederalDistrict.CENTRAL),
        FeedSource("АиФ Кострома", "https://kostroma.aif.ru/rss/all.php", Region.KOSTROMA, FederalDistrict.CENTRAL),
        FeedSource("Kostroma.Today", "https://kostroma.today/rss", Region.KOSTROMA, FederalDistrict.CENTRAL),
        FeedSource("Логос", "https://logos44.ru/rss", Region.KOSTROMA, FederalDistrict.CENTRAL),
        FeedSource("Смоленская народная газета", "https://smoldaily.ru/rss", Region.SMOLENSK, FederalDistrict.CENTRAL),
        FeedSource("Смоленская газета", "https://smolgazeta.ru/rss.xml", Region.SMOLENSK, FederalDistrict.CENTRAL),
        FeedSource("О чём говорит Смоленск", "https://smolensk-i.ru/feed", Region.SMOLENSK, FederalDistrict.CENTRAL),
        FeedSource("АиФ Тула", "https://tula.aif.ru/rss/all.php", Region.TULA, FederalDistrict.CENTRAL),
        FeedSource("71.ru", "https://71.ru/rss/", Region.TULA, FederalDistrict.CENTRAL),
        FeedSource("MySlo", "https://myslo.ru/rss/lenta/", Region.TULA, FederalDistrict.CENTRAL),
        FeedSource("RZN.info Тула", "https://rss.rzn.info/rss/news/tula.xml", Region.TULA, FederalDistrict.CENTRAL),
        FeedSource("АиФ Ярославль", "https://yar.aif.ru/rss/all.php", Region.YAROSLAVL, FederalDistrict.CENTRAL),
        FeedSource("76.ru", "https://76.ru/rss/", Region.YAROSLAVL, FederalDistrict.CENTRAL),
        FeedSource("Ярославский регион", "https://yarreg.ru/feed/", Region.YAROSLAVL, FederalDistrict.CENTRAL),
        FeedSource("Орёлград", "https://orelgrad.ru/feed/", Region.OREL, FederalDistrict.CENTRAL),
        FeedSource("ОрёлТаймс", "https://oreltimes.ru/feed/", Region.OREL, FederalDistrict.CENTRAL),
        FeedSource("Google Новости: Орловская область", googleNews("Орловская область новости"), Region.OREL, FederalDistrict.CENTRAL),
        FeedSource("Kursk.com", "https://kursk.com/rss", Region.KURSK, FederalDistrict.CENTRAL),
        FeedSource("Курские известия", "https://kursk-izvestia.ru/news/rss/", Region.KURSK, FederalDistrict.CENTRAL),
        FeedSource("46ТВ", "https://46tv.ru/rss.xml", Region.KURSK, FederalDistrict.CENTRAL),
        FeedSource("АиФ Белгород", "https://bel.aif.ru/rss/all.php", Region.BELGOROD, FederalDistrict.CENTRAL),
        FeedSource("МОЁ! Белгород", "https://www.moe-belgorod.ru/rss/", Region.BELGOROD, FederalDistrict.CENTRAL),
        FeedSource("Go31", "https://go31.ru/rss", Region.BELGOROD, FederalDistrict.CENTRAL),
        FeedSource("МОЁ! Воронеж", "https://www.moe-online.ru/rss/", Region.VORONEZH, FederalDistrict.CENTRAL),
        FeedSource("Вести Воронеж", "https://vestivrn.ru/rss/", Region.VORONEZH, FederalDistrict.CENTRAL),
        FeedSource("RZN.info Воронеж", "https://rss.rzn.info/rss/news/voronezh.xml", Region.VORONEZH, FederalDistrict.CENTRAL),
        FeedSource("Lipetsknews", "https://lipetsknews.ru/rss", Region.LIPETSK, FederalDistrict.CENTRAL),
        FeedSource("Медиалипецк", "https://medialipetsk.ru/rss", Region.LIPETSK, FederalDistrict.CENTRAL),
        FeedSource("48.ru", "https://48.ru/rss/", Region.LIPETSK, FederalDistrict.CENTRAL),
        FeedSource("RZN.info Липецк", "https://rss.rzn.info/rss/news/lipetsk.xml", Region.LIPETSK, FederalDistrict.CENTRAL),
        FeedSource("ВТамбове", "https://vtambove.ru/feed/", Region.TAMBOV, FederalDistrict.CENTRAL),
        FeedSource("Вести Тамбов", "https://vestitambov.ru/rss", Region.TAMBOV, FederalDistrict.CENTRAL),
        FeedSource("68.ru", "https://68.ru/rss/", Region.TAMBOV, FederalDistrict.CENTRAL),
        FeedSource("RZN.info Тамбов", "https://rss.rzn.info/rss/news/tambov.xml", Region.TAMBOV, FederalDistrict.CENTRAL),
        FeedSource("АиФ Брянск", "https://bryansk.aif.ru/rss/all.php", Region.BRYANSK, FederalDistrict.CENTRAL),
        FeedSource("БрянскToday", "https://bryansktoday.ru/export/rss", Region.BRYANSK, FederalDistrict.CENTRAL),
        FeedSource("RZN.info Брянск", "https://rss.rzn.info/rss/news/bryansk.xml", Region.BRYANSK, FederalDistrict.CENTRAL),
        FeedSource("RZN.info Рязань", "https://rss.rzn.info/rss/news/ryazan.xml", Region.RYAZAN, FederalDistrict.CENTRAL),
        FeedSource("Рязанские ведомости", "https://rv-ryazan.ru/feed/", Region.RYAZAN, FederalDistrict.CENTRAL),
        FeedSource("62.ru", "https://62.ru/rss/", Region.RYAZAN, FederalDistrict.CENTRAL),

        // Московская область (Москва и МО)
        FeedSource("Подмосковье сегодня", "https://mosregtoday.ru/rss/", Region.MOSCOW_OBLAST, FederalDistrict.MOSCOW_OBLAST),
        FeedSource("360°", "https://360tv.ru/rss/", Region.MOSCOW_OBLAST, FederalDistrict.MOSCOW_OBLAST),
        FeedSource("РИАМО", "https://riamo.ru/rss", Region.MOSCOW_OBLAST, FederalDistrict.MOSCOW_OBLAST),
        FeedSource("Москва 24", "https://www.m24.ru/rss.xml", Region.MOSCOW_OBLAST, FederalDistrict.MOSCOW_OBLAST),
        FeedSource("Вечерняя Москва", "https://vm.ru/rss", Region.MOSCOW_OBLAST, FederalDistrict.MOSCOW_OBLAST),
        FeedSource("МК", "https://www.mk.ru/rss/index.xml", Region.MOSCOW_OBLAST, FederalDistrict.MOSCOW_OBLAST),

        // Приволжье
        FeedSource("АиФ Самара", "https://samara.aif.ru/rss/all.php", Region.SAMARA, FederalDistrict.VOLGA),
        FeedSource("63.ru", "https://63.ru/rss/", Region.SAMARA, FederalDistrict.VOLGA),
        FeedSource("Волга Ньюс", "https://volga.news/rss", Region.SAMARA, FederalDistrict.VOLGA),
        FeedSource("Другой город", "https://drugoigorod.ru/feed/", Region.SAMARA, FederalDistrict.VOLGA),
        FeedSource("НИА Нижний Новгород", "https://niann.ru/rss", Region.NIZHNY_NOVGOROD, FederalDistrict.VOLGA),
        FeedSource("NN.ru", "https://www.nn.ru/rss-feeds/rss.xml", Region.NIZHNY_NOVGOROD, FederalDistrict.VOLGA),
        FeedSource("Открытый Нижний", "https://www.opennov.ru/rss/", Region.NIZHNY_NOVGOROD, FederalDistrict.VOLGA),
        FeedSource("52.ru", "https://52.ru/rss/", Region.NIZHNY_NOVGOROD, FederalDistrict.VOLGA),
        FeedSource("KazanFirst", "https://kazanfirst.ru/rss", Region.TATARSTAN, FederalDistrict.VOLGA),
        FeedSource("Татар-информ", "https://tatar-inform.tatar/rss/", Region.TATARSTAN, FederalDistrict.VOLGA),
        FeedSource("116.ru", "https://116.ru/rss/", Region.TATARSTAN, FederalDistrict.VOLGA),
        FeedSource("АиФ Саратов", "https://saratov.aif.ru/rss/all.php", Region.SARATOV, FederalDistrict.VOLGA),
        FeedSource("Саратовские новости", "https://saratovnews.ru/rss", Region.SARATOV, FederalDistrict.VOLGA),
        FeedSource("SARBC", "https://www.sarbc.ru/rss/data-utf/main.rss", Region.SARATOV, FederalDistrict.VOLGA),
        FeedSource("Улпресса", "https://ulpressa.ru/feed/", Region.ULYANOVSK, FederalDistrict.VOLGA),
        FeedSource("73online", "https://73online.ru/multi.rss.php", Region.ULYANOVSK, FederalDistrict.VOLGA),
        FeedSource("Google Новости: Ульяновская область", googleNews("Ульяновская область новости"), Region.ULYANOVSK, FederalDistrict.VOLGA),
        FeedSource("АиФ Киров", "https://kirov.aif.ru/rss/all.php", Region.KIROV, FederalDistrict.VOLGA),
        FeedSource("Pro Город Киров", "https://progorod43.ru/rss.xml", Region.KIROV, FederalDistrict.VOLGA),
        FeedSource("43.ru", "https://43.ru/rss/", Region.KIROV, FederalDistrict.VOLGA),
        FeedSource("АиФ Пенза", "https://penza.aif.ru/rss/all.php", Region.PENZA, FederalDistrict.VOLGA),
        FeedSource("PenzaNews", "https://penzanews.ru/rss/", Region.PENZA, FederalDistrict.VOLGA),
        FeedSource("Google Новости: Пензенская область", googleNews("Пензенская область новости"), Region.PENZA, FederalDistrict.VOLGA),
        FeedSource("АиФ Удмуртия", "https://udm.aif.ru/rss/all.php", Region.UDMURTIA, FederalDistrict.VOLGA),
        FeedSource("Google Новости: Удмуртия", googleNews("Удмуртия новости"), Region.UDMURTIA, FederalDistrict.VOLGA),
        FeedSource("МордовМедиа", "https://www.mordovmedia.ru/rss/", Region.MORDOVIA, FederalDistrict.VOLGA),
        FeedSource("Известия Мордовии", "https://izvmor.ru/feed/", Region.MORDOVIA, FederalDistrict.VOLGA),
        FeedSource("Google Новости: Мордовия", googleNews("Мордовия новости"), Region.MORDOVIA, FederalDistrict.VOLGA),
        FeedSource("Google Новости: Марий Эл", googleNews("Марий Эл новости"), Region.MARI_EL, FederalDistrict.VOLGA),
        FeedSource("МЭТР", "https://metr12.ru/rss.xml", Region.MARI_EL, FederalDistrict.VOLGA),
        FeedSource("Ваш Новый День", "https://vnd12.ru/rss.xml", Region.MARI_EL, FederalDistrict.VOLGA),
        FeedSource("Google Новости: Чувашия", googleNews("Чувашия новости"), Region.CHUVASHIA, FederalDistrict.VOLGA),
        FeedSource("Грани", "https://www.grani21.ru/rss.xml", Region.CHUVASHIA, FederalDistrict.VOLGA),
        FeedSource("Республика", "https://respublika21.ru/?format=feed&type=rss", Region.CHUVASHIA, FederalDistrict.VOLGA),

        // Сибирь
        FeedSource("АиФ Новосибирск", "https://nsk.aif.ru/rss/all.php", Region.NOVOSIBIRSK, FederalDistrict.SIBERIA),
        FeedSource("НГС Новосибирск", "https://ngs.ru/rss-feeds/rss.xml", Region.NOVOSIBIRSK, FederalDistrict.SIBERIA),
        FeedSource("Прецедент", "https://precedent.tv/rss", Region.NOVOSIBIRSK, FederalDistrict.SIBERIA),
        FeedSource("АиФ Красноярск", "https://krsk.aif.ru/rss/all.php", Region.KRASNOYARSK, FederalDistrict.SIBERIA),
        FeedSource("НГС24 Красноярск", "https://ngs24.ru/rss-feeds/rss.xml", Region.KRASNOYARSK, FederalDistrict.SIBERIA),
        FeedSource("Google Новости: Красноярский край", googleNews("Красноярский край новости"), Region.KRASNOYARSK, FederalDistrict.SIBERIA),
        FeedSource("АиФ Омск", "https://omsk.aif.ru/rss/all.php", Region.OMSK, FederalDistrict.SIBERIA),
        FeedSource("ОмскРегион", "https://omskregion.info/rss.xml", Region.OMSK, FederalDistrict.SIBERIA),
        FeedSource("СуперОмск", "https://superomsk.ru/rss", Region.OMSK, FederalDistrict.SIBERIA),
        FeedSource("НГС55 Омск", "https://ngs55.ru/rss-feeds/rss.xml", Region.OMSK, FederalDistrict.SIBERIA),
        FeedSource("АиФ Томск", "https://tomsk.aif.ru/rss/all.php", Region.TOMSK, FederalDistrict.SIBERIA),
        FeedSource("Томский обзор", "https://obzor.city/rss/alltomsk.xml", Region.TOMSK, FederalDistrict.SIBERIA),
        FeedSource("НГС70 Томск", "https://ngs70.ru/rss-feeds/rss.xml", Region.TOMSK, FederalDistrict.SIBERIA),
        FeedSource("Сибдепо", "https://sibdepo.ru/rss", Region.KEMEROVO, FederalDistrict.SIBERIA),
        FeedSource("Газета Кемерова", "https://gazeta.a42.ru/rss", Region.KEMEROVO, FederalDistrict.SIBERIA),
        FeedSource("Кузбасс", "https://kuzbass85.ru/rss", Region.KEMEROVO, FederalDistrict.SIBERIA),
        FeedSource("Город N", "https://city-n.ru/rss", Region.KEMEROVO, FederalDistrict.SIBERIA),
        FeedSource("НГС42 Кемерово", "https://ngs42.ru/rss-feeds/rss.xml", Region.KEMEROVO, FederalDistrict.SIBERIA),
        FeedSource("АиФ Иркутск", "https://irk.aif.ru/rss/all.php", Region.IRKUTSK, FederalDistrict.SIBERIA),
        FeedSource("НГС Иркутск", "https://ngs38.ru/rss/", Region.IRKUTSK, FederalDistrict.SIBERIA),
        FeedSource("ИрСити", "https://ircity.ru/rss-feeds/rss.xml", Region.IRKUTSK, FederalDistrict.SIBERIA),
        FeedSource("Google Новости: Иркутская область", googleNews("Иркутская область новости"), Region.IRKUTSK, FederalDistrict.SIBERIA),
        FeedSource("Алтапресс", "https://altapress.ru/rss", Region.ALTAI, FederalDistrict.SIBERIA),
        FeedSource("Толк", "https://tolknews.ru/rss.xml", Region.ALTAI, FederalDistrict.SIBERIA),
        FeedSource("Банкфакс", "https://bankfax.ru/rss.xml", Region.ALTAI, FederalDistrict.SIBERIA),
        FeedSource("НГС22 Барнаул", "https://ngs22.ru/rss-feeds/rss.xml", Region.ALTAI, FederalDistrict.SIBERIA),
        FeedSource("АиФ Чита", "https://chita.aif.ru/rss/all.php", Region.ZABAIKALYE, FederalDistrict.SIBERIA),
        FeedSource("Чита.Ру", "https://chita.ru/rss-feeds/rss.xml", Region.ZABAIKALYE, FederalDistrict.SIBERIA),
        FeedSource("Google Новости: Забайкальский край", googleNews("Забайкальский край новости"), Region.ZABAIKALYE, FederalDistrict.SIBERIA),
        FeedSource("Информ Полис", "https://www.infpol.ru/rss/", Region.BURYATIA, FederalDistrict.SIBERIA),
        FeedSource("Байкал-Daily", "https://baikal-daily.ru/news/rss/", Region.BURYATIA, FederalDistrict.SIBERIA),
        FeedSource("Аригус", "https://arigus.tv/rss", Region.BURYATIA, FederalDistrict.SIBERIA),
        FeedSource("Туваонлайн", "https://tuvaonline.ru/rss", Region.TYVA, FederalDistrict.SIBERIA),
        FeedSource("Тува.Азия", "https://www.tuva.asia/rss.xml", Region.TYVA, FederalDistrict.SIBERIA),
        FeedSource("Google Новости: Тыва", googleNews("Тыва новости"), Region.TYVA, FederalDistrict.SIBERIA),
        FeedSource("Google Новости: Хакасия", googleNews("Хакасия новости"), Region.KHAKASSIA, FederalDistrict.SIBERIA),
        FeedSource("Хакасия", "https://khakasia.info/feed/", Region.KHAKASSIA, FederalDistrict.SIBERIA),
        FeedSource("Шанс", "https://shansonline.ru/index.php?format=feed&type=rss", Region.KHAKASSIA, FederalDistrict.SIBERIA),
        FeedSource("19rus", "https://19rusinfo.ru/?format=feed&type=rss", Region.KHAKASSIA, FederalDistrict.SIBERIA),
    )

    fun forDistrict(district: FederalDistrict): List<FeedSource> =
        all.filter { it.district == district }
}
