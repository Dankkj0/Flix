package com.example // Altere para o package correto do seu projeto

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.ExtractorApiKt.loadExtractor
import org.jsoup.nodes.Element

class PobreFlixProvider : MainAPI() {
    override var name = "PobreFlix"
    override var mainUrl = "https://www.pobreflixtv.design"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    
    // 1. FUNÇÃO DE BUSCA: O que acontece quando você pesquisa no app
    override suspend fun search(query: String): List<SearchResponse> {
        // O site usa a estrutura padrão /?s=termo_de_busca
        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document
        
        // O Jsoup varre o HTML procurando os cards dos filmes
        // Você precisa inspecionar o site para confirmar a classe CSS dos itens (ex: .poster, .item)
        return document.select("div.items article, div.poster").mapNotNull { 
            it.toSearchResult()
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3, .title, a")?.text() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src")

        // Retorna se é filme ou série com base na URL ou tags do site
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

    // 2. CARREGAR PÁGINA DO FILME: Pega sinopse, episódios, etc.
    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1, .title")?.text() ?: return null
        val poster = document.selectFirst(".poster img")?.attr("src")
        val description = document.selectFirst(".description, .sinopse")?.text()

        return if (url.contains("/series/")) {
            // Lógica para listar temporadas e episódios se for série
            val episodes = mutableListOf<Episode>()
            document.select(".episodios, .list-episodes a").forEach { 
                // Varre os links de episódios da página
                val epHref = it.attr("href")
                val epName = it.text()
                episodes.add(Episode(epHref, name = epName))
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

    // 3. EXTRAIR LINKS DE VÍDEO: Encontra o player limpo (.mp4/.m3u8)
    override suspend fun loadLinks(
        data: String,
        isCaster: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        // O PobreFlix costuma usar iframes de players externos (ex: Player, Mixdrop, Streamtape)
        // Precisamos pegar a URL desse iframe
        document.select("iframe, .player-embed iframe").forEach { 
            val iframeUrl = it.attr("src")
            
            // O Cloudstream já tem extratores automáticos para vários servidores conhecidos!
            // Se o link for de um servidor suportado, ele extrai o vídeo sozinho:
            loadExtractor(iframeUrl, data, subtitleCallback, callback)
        }
        return true
    }
}
