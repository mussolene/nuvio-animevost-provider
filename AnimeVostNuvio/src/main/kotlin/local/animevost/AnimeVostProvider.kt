package local.animevost

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.jsoup.nodes.Element

class AnimeVostProvider : MainAPI() {
    override var mainUrl = "https://animevost.org"
    override var name = "AnimeVost"
    override var lang = "ru"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie)

    private val requestHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Linux; Android 10; Android TV) " +
            "AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36",
        "Accept-Language" to "ru-RU,ru;q=0.9,en;q=0.7"
    )

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.post(
            "$mainUrl/index.php?do=search",
            data = mapOf(
                "do" to "search",
                "subaction" to "search",
                "search_start" to "0",
                "full_search" to "0",
                "result_from" to "1",
                "story" to query
            ),
            headers = requestHeaders
        )
        return response.document.select(
            "#dle-content div.shortstory, #dle-content div.searchnews, " +
                "#dle-content div.searchitem"
        ).mapNotNull(::toSearchResult).distinctBy { it.url }
    }

    private fun toSearchResult(card: Element): SearchResponse? {
        val link = card.selectFirst(".shortstoryHead h1 a, .shortstoryHead h2 a, a[href*='/tip/']")
            ?: return null
        val url = fixUrl(link.attr("href"))
        if (!url.contains("/tip/")) return null
        val title = card.selectFirst("h4")?.text()?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: AnimeVostParser.cleanTitle(link.text())
        if (title.isEmpty()) return null
        val poster = card.selectFirst("img.imgRadius")?.attr("src")?.let(::fixUrlNull)
        val type = if (url.contains("/polnometrazhnyy-film/")) TvType.AnimeMovie else TvType.Anime
        return newAnimeSearchResponse(title, url, type) {
            posterUrl = poster
            addDubStatus(dubExist = true, subExist = false)
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url, headers = requestHeaders).document
        val content = document.selectFirst("#dle-content div.shortstory")
            ?: error("AnimeVost не вернул карточку релиза")
        val title = content.selectFirst("h4[itemprop=name], h4")?.text()?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: AnimeVostParser.cleanTitle(
                content.selectFirst(".shortstoryHead h1, .shortstoryHead h2")?.text().orEmpty()
            )
        val poster = content.selectFirst("img.imgRadius")?.attr("src")?.let(::fixUrlNull)
        val description = content.selectFirst("span[itemprop=description]")?.text()?.trim()
        val year = metadataValue(content, "Год выхода")?.toIntOrNull()
        val episodes = AnimeVostParser.episodes(document.html()).mapIndexed { index, item ->
            newEpisode(mapper.writeValueAsString(EpisodeData(item.id, url))) {
                name = item.name
                season = 1
                episode = episodeNumber(item.name) ?: index + 1
                posterUrl = "https://media.animetop.info/img/${item.id}.jpg"
            }
        }

        return if (url.contains("/polnometrazhnyy-film/") && episodes.size == 1) {
            newMovieLoadResponse(title, url, TvType.AnimeMovie, episodes.first().data) {
                posterUrl = poster
                this.year = year
                plot = description
            }
        } else {
            newTvSeriesLoadResponse(title, url, TvType.Anime, episodes) {
                posterUrl = poster
                this.year = year
                plot = description
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val episode = mapper.readValue(data, EpisodeData::class.java) ?: return false
        val playerUrl = "$mainUrl/frame5.php?play=${episode.id}&old=1"
        val html = app.get(
            playerUrl,
            headers = requestHeaders,
            referer = episode.referer
        ).text
        val streams = AnimeVostParser.streams(html)
        streams.forEach { stream ->
            callback(
                newExtractorLink(
                    source = name,
                    name = "$name ${stream.label}",
                    url = stream.url,
                    type = ExtractorLinkType.VIDEO
                ) {
                    referer = playerUrl
                    quality = stream.quality
                    headers = requestHeaders
                }
            )
        }
        return streams.isNotEmpty()
    }

    private fun metadataValue(content: Element, label: String): String? = content
        .select(".shortstoryContent p")
        .firstOrNull { it.selectFirst("strong")?.text()?.trim()?.startsWith(label) == true }
        ?.clone()
        ?.also { it.select("strong").remove() }
        ?.text()
        ?.trim()

    private fun episodeNumber(name: String): Int? = Regex("""\d+""")
        .find(name)
        ?.value
        ?.toIntOrNull()

    private data class EpisodeData(
        @JsonProperty("id") val id: String,
        @JsonProperty("referer") val referer: String
    )
}
