package local.animevost

internal object AnimeVostParser {
    private val episodeDataRegex = Regex("""var\s+data\s*=\s*(\{.*?})\s*;""", RegexOption.DOT_MATCHES_ALL)
    private val episodeRegex = Regex("""["']([^"']+)["']\s*:\s*["'](\d+)["']""")
    private val fileRegex = Regex("""["']file["']\s*:\s*["']([^"']+)["']""")
    private val qualityRegex = Regex("""\[([^]]+)](.+?)(?=,\[|$)""")

    data class EpisodeEntry(val name: String, val id: String)
    data class StreamEntry(val label: String, val quality: Int, val url: String)

    fun episodes(html: String): List<EpisodeEntry> {
        val objectText = episodeDataRegex.find(html)?.groupValues?.get(1) ?: return emptyList()
        return episodeRegex.findAll(objectText).map { match ->
            EpisodeEntry(match.groupValues[1].trim(), match.groupValues[2])
        }.toList()
    }

    fun streams(html: String): List<StreamEntry> {
        val fileData = fileRegex.findAll(html)
            .map { it.groupValues[1] }
            .filter { it.contains("http") }
            .maxByOrNull { it.length }
            ?: return emptyList()

        return qualityRegex.findAll(fileData).mapNotNull { match ->
            val label = match.groupValues[1].trim()
            val quality = Regex("""(\d{3,4})[pр]?""")
                .find(label)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
                ?: return@mapNotNull null
            val primaryUrl = match.groupValues[2]
                .split(" or ")
                .asSequence()
                .map(String::trim)
                .firstOrNull { it.startsWith("https://") }
                ?: return@mapNotNull null
            StreamEntry(label, quality, primaryUrl)
        }.distinctBy { it.quality }.toList()
    }

    fun cleanTitle(value: String): String = value
        .replace(Regex("""\s*\[[^]]+\s+из\s+[^]]+]\s*$""", RegexOption.IGNORE_CASE), "")
        .trim()
}
