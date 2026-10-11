package local.animevost

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimeVostParserTest {
    @Test
    fun parsesEpisodesWithTrailingComma() {
        val html = """
            <script>var data = {"1 серия":"184891155","2 серия":"1657804828",};</script>
        """.trimIndent()

        assertEquals(
            listOf(
                AnimeVostParser.EpisodeEntry("1 серия", "184891155"),
                AnimeVostParser.EpisodeEntry("2 серия", "1657804828")
            ),
            AnimeVostParser.episodes(html)
        )
    }

    @Test
    fun parsesPrimaryUrlForEveryQuality() {
        val html = """
            <script>
            new Playerjs({
              "file":"[SD (480p)]https://cdn.example/480/video.mp4 or https://mirror.example/480.mp4,[HD (720р)]https://cdn.example/720/video.mp4,[FHD (1080p)]https://cdn.example/1080/video.mp4 or https://mirror.example/720.mp4"
            });
            </script>
        """.trimIndent()

        val streams = AnimeVostParser.streams(html)

        assertEquals(listOf(480, 720, 1080), streams.map { it.quality })
        assertEquals("https://cdn.example/1080/video.mp4", streams.last().url)
        assertTrue(streams.none { it.url.contains("mirror.example") })
    }

    @Test
    fun choosesLongestFileDefinition() {
        val html = """
            <script>new Playerjs({"file":"[SD (480p)]https://cdn.example/480.mp4"});</script>
            <script>new Playerjs({"file":"[SD (480p)]https://cdn.example/480.mp4,[FHD (1080p)]https://cdn.example/1080.mp4"});</script>
        """.trimIndent()

        assertEquals(listOf(480, 1080), AnimeVostParser.streams(html).map { it.quality })
    }

    @Test
    fun removesEpisodeRangeFromTitle() {
        assertEquals(
            "Аватар: Семь убежищ / Avatar: Seven Havens",
            AnimeVostParser.cleanTitle("Аватар: Семь убежищ / Avatar: Seven Havens [1-2 из 13]")
        )
    }
}
