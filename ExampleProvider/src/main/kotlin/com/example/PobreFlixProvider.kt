package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class PobreFlixProvider : MainAPI() {
    override var name = "PobreFlix"
    override var mainUrl = "https://www.pobreflixtv.design"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    
    // 1. FUNÇÃO DE BUSCA: Pesquisa no app
    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document
        
        return document.select("div.items article, div.poster").mapNotNull { element ->
            element.toSearchResult()
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3, .title, a")?.text() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src")

        return if (href.contains("/series/")) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }
    }

    // 2. CARREGAR PÁGINA DO FILME: Pega sinopse e episódios se for série
    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1, .title")?.text() ?: return null
        val poster = document.selectFirst(".poster img")?.attr("src")
        val description = document.selectFirst(".description, .sinopse")?.text()

        return if (url.contains("/series/")) {
            val episodes = mutableListOf<Episode>()
            
            document.select(".episodios, .list-episodes a").forEach { element -> 
                val epHref = element.attr("href")
                val epName = element.text()
                if (epHref.isNotEmpty()) {
                    episodes.add(Episode(epHref, name = epName))
                }
            }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
            }
        }
    }

    // 3. EXTRAIR LINKS DE VÍDEO: Encontra o player limpo
    override suspend fun loadLinks(
        data: String,
        isCaster: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        document.select("iframe, .player-embed iframe").forEach { element ->
            val iframeUrl = element.attr("src")
            if (iframeUrl.isNotEmpty()) {
                // CORREÇÃO DEFINITIVA: Usando o objeto global ExtractorApi do Cloudstream
                ExtractorApi().loadExtractor(iframeUrl, data, subtitleCallback, callback)
            }
        }
        return true
    }
}
