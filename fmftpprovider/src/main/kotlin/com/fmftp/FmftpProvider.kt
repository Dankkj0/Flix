package com.fmftp

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

@Suppress("UNCHECKED_CAST")
class FmftpProvider : MainAPI() {
    override var name = "Fmftp BDIX"
    override var lang = "bn"
    override var mainUrl = "https://fmftp.net"
    override val hasMainPage = true
    override val hasQuickSearch = true

    private val apiUrl = "$mainUrl/api/movies?limit=2000&sort=release_date"
    private val mapper = jacksonObjectMapper()

    companion object {
        val movieStore = mutableMapOf<String, MovieData>()
        val libraryMap = mutableMapOf<String, String>()
    }

    data class MovieData(
        val title: String,
        val streamUrl: String,
        val poster: String,
        val backdrop: String,
        val plot: String,
        val year: Int?,
        val rating: Double?,
        val genres: List<String>,
        val cast: List<String>
    )

    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Accept" to "application/json"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val allMovies = fetchAllMovies()
        if (allMovies.isEmpty()) return newHomePageResponse(emptyList())

        val grouped = mutableMapOf<String, MutableList<SearchResponse>>()
        val latest = mutableListOf<SearchResponse>()

        for ((detailUrl, searchResp) in allMovies) {
            val lib = libraryMap[detailUrl] ?: "Unknown"
            grouped.getOrPut(lib) { mutableListOf() }.add(searchResp)
            if (latest.size < 30) latest.add(searchResp)
        }

        val lists = mutableListOf<HomePageList>()
        if (latest.isNotEmpty()) lists.add(HomePageList("Latest Movies", latest))

        val order = listOf("Hollywood", "Bollywood", "Hindi dubbed", "Indian Bangla")
        for (cat in order) {
            grouped[cat]?.let { lists.add(HomePageList("$cat Movies", it)) }
        }

        return newHomePageResponse(lists)
    }

    private suspend fun fetchAllMovies(): List<Pair<String, SearchResponse>> {
        return try {
            val response = app.get(apiUrl, headers = headers).text
            val json = mapper.readValue<Map<String, Any>>(response)
            val movies = json["data"] as? List<Map<String, Any>> ?: return emptyList()

            movies.mapNotNull { movie ->
                val id = movie["id"]?.toString() ?: return@mapNotNull null
                val title = movie["title"] as? String ?: return@mapNotNull null
                val year = (movie["year"] as? String)?.toIntOrNull()
                val plot = movie["overview"] as? String ?: ""
                val rating = (movie["online_rating"] as? Number)?.toDouble()
                val genreStr = movie["genre"] as? String ?: ""
                val genres = genreStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val castsStr = movie["casts"] as? String ?: ""
                val castList = castsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val library = (movie["Library"] as? Map<String, Any>)?.get("name") as? String ?: "Unknown"
                val relativeUrl = movie["url"] as? String ?: ""
                val streamUrl = "$mainUrl$relativeUrl"
                val poster = ""

                val detailUrl = "http://fmftp.local/$id"
                movieStore[detailUrl] = MovieData(
                    title, streamUrl, poster, "", plot, year, rating, genres, castList
                )
                libraryMap[detailUrl] = library

                val searchResp = newMovieSearchResponse(
                    title,        // name
                    detailUrl,     // url
                    TvType.Movie,  // type
                    false          // isAd
                ) {
                    this.posterUrl = poster
                    this.year = year
                }
                Pair(detailUrl, searchResp)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val all = fetchAllMovies()
        return all.mapNotNull { (_, resp) ->
            if (resp.name?.contains(query, ignoreCase = true) == true) resp else null
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val movie = movieStore[url] ?: throw Error("Movie not found")
        return newMovieLoadResponse(
            name = movie.title,
            url = url,
            type = TvType.Movie,
            data = movie.streamUrl
        ) {
            this.plot = movie.plot
            this.year = movie.year
            this.posterUrl = movie.poster
            this.backgroundPosterUrl = movie.backdrop
            // score is optional; we skip to avoid type mismatch
            val tagsList = mutableListOf<String>()
            tagsList.addAll(movie.genres)
            if (movie.cast.isNotEmpty()) {
                tagsList.add("Cast: ${movie.cast.joinToString(", ")}")
            }
            if (tagsList.isNotEmpty()) this.tags = tagsList
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val streamUrl = data
        val quality = when {
            streamUrl.contains("1080") -> 1080
            streamUrl.contains("720") -> 720
            streamUrl.contains("480") -> 480
            else -> 0
        }
        callback.invoke(
            newExtractorLink(
                source = name,
                name = "Direct",
                url = streamUrl
            ) {
                this.referer = mainUrl
                this.quality = quality
            }
        )
        return true
    }
}