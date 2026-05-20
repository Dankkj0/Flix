package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class TuDoramaProvider : MainAPI() {
    override var mainUrl = "https://tudorama.com"
    override var name = "TuDorama"
    override val supportedTypes = setOf(TvType.AsianDrama)
    override var lang = "es"
    override val hasMainPage = true

    // Categorías que aparecen en el home de la app
    override val mainPage = mainPageOf(
        "$mainUrl/genero/series/page/" to "K-Dramas",
        "$mainUrl/genero/cdrama/page/" to "C-Dramas",
        "$mainUrl/genero/jdrama/page/" to "J-Dramas",
        "$mainUrl/genero/emision/page/" to "En Emisión",
        "$mainUrl/genero/peliculas/page/" to "Películas",
    )

    // Convierte un elemento HTML de la lista en un SearchResponse
    private fun Element.toSearchResponse(): SearchResponse? {
        val title = this.selectFirst("h3 a, h2 a")?.text() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val poster = this.selectFirst("img")?.attr("src")
        return newTvSeriesSearchResponse(title, href, TvType.AsianDrama) {
            this.posterUrl = poster
        }
    }

    // Carga cada categoría del home con paginación
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = "${request.data}$page/"
        val document = app.get(url).document
        val items = document.select("article, div.item, div.TPost")
            .mapNotNull { it.toSearchResponse() }
        return newHomePageResponse(request.name, items)
    }

    // Búsqueda
    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("article, div.item, div.TPost")
            .mapNotNull { it.toSearchResponse() }
    }

    // Página de detalle de la serie
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("h1, h2.Title")?.text() ?: "Sin título"
        val poster = document.selectFirst("div.Image img, img.Poster")?.attr("src")
        val plot = document.selectFirst("div.Description p, div.sinopsis")?.text()
        val tags = document.select("div.Genre a, a[href*='/genres/']").map { it.text() }
        val year = document.selectFirst("span.Date, span.year")?.text()?.trim()?.toIntOrNull()

        // Scrapea los episodios
        val episodes = document.select("li a[href*='/ver/']").mapNotNull { el ->
            val epUrl = el.attr("href")
            val epName = el.selectFirst("h3, span")?.text() ?: el.text()
            // Intenta extraer numero de temporada y episodio de la URL
            val match = Regex("""s(\d+)x(\d+)""").find(epUrl)
            val season = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val epNum = match?.groupValues?.get(2)?.toIntOrNull()
            newEpisode(epUrl) {
                this.name = epName
                this.season = season
                this.episode = epNum
            }
        }

        return newTvSeriesLoadResponse(title, url, TvType.AsianDrama, episodes) {
            this.posterUrl = poster
            this.plot = plot
            this.tags = tags
            this.year = year
        }
    }

    // Extrae los links de video del episodio
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        // Busca los iframes del reproductor
        document.select("iframe").forEach { iframe ->
            val src = iframe.attr("src").ifEmpty { iframe.attr("data-src") }
            if (src.isNotEmpty()) {
                loadExtractor(src, data, subtitleCallback, callback)
            }
        }

        // También intenta los links de descarga directa como fallback
        document.select("table a[href*='cdn.tudorama.com']").forEach { link ->
            val href = link.attr("href")
            loadExtractor(href, data, subtitleCallback, callback)
        }

        return true
    }
}