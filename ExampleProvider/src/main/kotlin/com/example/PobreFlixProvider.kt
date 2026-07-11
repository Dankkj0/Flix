package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class PobreFlixProvider : MainAPI() {
    override var name = "PobreFlix"
    override var mainUrl = "https://www.pobreflixtv.design"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    
    // 1. CORREÇÃO DA BUSCA: Rota correta é /buscar?q=...
    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/buscar?q=$query"
        val document = app.get(url).document
        
        // Seleciona os cards de filmes/séries na listagem (geralmente estruturados em article ou div de itens)
        return document.select("div.items article, div.poster, .dropdown-grid a, main .container a").mapNotNull { element ->
            element.toSearchResult()
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val href = this.attr("href") ?: return null
        if (!href.contains("/filme/") && !href.contains("/serie/")) return null
        
        val title = this.selectFirst("h3, .title, span")?.text() ?: this.text() ?: "Sem título"
        val posterUrl = this.selectFirst("img")?.attr("data-src") 
            ?: this.selectFirst("img")?.attr("src")

        // Correção definitiva: URLs usam "/serie/" no singular no padrão do site
        return if (href.contains("/serie/")) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }
    }

    // 2. CARREGAR PÁGINA DO CONTEÚDO (Filme ou Série)
    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        
        // Pega do cabeçalho principal h1 ou meta tags limpas
        val title = document.selectFirst("h1, .filme-meta h1")?.text() 
            ?: document.selectFirst("meta[property=og:title]")?.attr("content")?.replace(" - Assistir Online Grátis", "")
            ?: return null
            
        val poster = document.selectFirst("img.filme-poster")?.attr("src") 
            ?: document.selectFirst("meta[property=og:image]")?.attr("content")
            
        // Extrai a sinopse baseada nas tags fortes identificadas no HTML
        val description = document.selectFirst(".filme-informacoes p:contains(Sinopse:), .conteudo-seo p")?.text()
            ?.replace("Sinopse:", "")?.trim()

        return if (url.contains("/serie/")) {
            val episodes = mutableListOf<Episode>()
            
            // O site agrupa episódios ou temporadas no HTML ou dentro de blocos de links
            // Como as séries possuem múltiplos episódios mapeados via SEO/hasPart ou listas:
            document.select(".seo-links-list a, .list-episodes a").forEach { element -> 
                val epHref = element.attr("href")
                val epName = element.text()
                // Garante que estamos pegando links válidos de episódios
                if (epHref.isNotEmpty() && (epHref.contains("/serie/") || epHref.contains("temporada"))) {
                    episodes.add(Episode(fixUrl(epHref), name = epName))
                }
            }
            
            // Se a lista de links falhar, tenta ler a estrutura de dados estruturados (Schema json)
            if (episodes.isEmpty()) {
                document.select("script[type=application/ld+json]").forEach { script ->
                    if (script.data().contains("TVSeason") || script.data().contains("hasPart")) {
                        // Regex simples para capturar links alternativos de temporadas/episódios no Json
                        "\"@id\":\\s*\"(https.*?)\"".toRegex().findAll(script.data()).forEach { match ->
                            val epUrl = match.groupValues[1].replace("\\/", "/")
                            if (!epUrl.endsWith("#series")) {
                                episodes.add(Episode(epUrl, name = "Acessar Temporada / Episódio"))
                            }
                        }
                    }
                }
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes.distinctBy { it.data }) {
                this.posterUrl = poster
                this.plot = description
            }
        } else {
            // Se for filme, a própria URL da página é passada para o extrator de links
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
            }
        }
    }

    // 3. EXTRAIR LINKS DE VÍDEO (Dos botões de canais Dublado/Legendado)
    override suspend fun loadLinks(
        data: String,
        isCaster: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        // CORREÇÃO: O site usa botões com "data-url" para os players (ex: plenoflu, vidsrc)
        // O iframe nativo fica vazio até o clique, então precisamos ler os atributos "data-url" dos botões de canais
        val playerElements = document.select("button[data-url], .canal-opcoes button, iframe[src]")
        
        var foundLinks = false
        
        playerElements.forEach { element ->
            val videoUrl = element.attr("data-url").ifEmpty { element.attr("src") }
            
            if (videoUrl.isNotEmpty() && !videoUrl.startsWith("about:")) {
                // Executa os extratores conhecidos do Cloudstream para resolver a URL (ex: vidsrc)
                loadExtractor(videoUrl, data, subtitleCallback, callback)
                foundLinks = true
            }
        }
        
        return foundLinks
    }
}
