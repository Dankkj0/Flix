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

    private val mapper = jacksonObjectMapper()

    companion object {
        val movieStore = mutableMapOf<String, MovieData>()
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

    // ------------------------------------------------------------
    // Main page – fetch each category with its own library ID
    // ------------------------------------------------------------
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val lists = mutableListOf<HomePageList>()

        // Helper to add a category
        suspend fun addCategory(name: String, libraryId: Int? = null) {
            val url = if (libraryId != null) {
                "$mainUrl/api/movies?limit=2000&library=$libraryId&sort=release_date"
            } else {
                "$mainUrl/api/movies?limit=2000&sort=release_date"
            }
            val movies = fetchMovies(url)
            if (movies.isNotEmpty()) lists.add(HomePageList(name, movies))
        }

        // 1. Latest Movies (no library filter) – shown first
        addCategory("Latest Movies")

        // 2. Hollywood – library=2
        addCategory("Hollywood Movies", 2)

        // 3. Bollywood – library=1
        addCategory("Bollywood Movies", 1)

        // 4. Hindi Dubbed – library=5
        addCategory("Hindi Dubbed Movies", 5)

        // 5. Bangla – library=7
        addCategory("Bangla Movies", 7)

        // 6. Animation – library=3
        addCategory("Animation Movies", 3)

        return newHomePageResponse(lists)
    }

    // ------------------------------------------------------------
    // Generic movie fetcher that accepts any API URL
    // ------------------------------------------------------------
    private suspend fun fetchMovies(apiUrl: String): List<SearchResponse> {
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
                val relativeUrl = movie["url"] as? String ?: ""
                val streamUrl = "$mainUrl$relativeUrl"

                // Extract images directly from JSON (the API provides them)
                val poster = movie["poster_url"] as? String ?: ""
                val backdrop = movie["backdrop_url"] as? String ?: ""

                val detailUrl = "http://fmftp.local/$id"
                movieStore[detailUrl] = MovieData(
                    title, streamUrl, poster, backdrop, plot, year, rating, genres, castList
                )

                newMovieSearchResponse(title, detailUrl, TvType.Movie, false) {
                    this.posterUrl = poster
                    this.year = year
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ------------------------------------------------------------
    // Search – fetch all movies from the main API (no filter)
    // ------------------------------------------------------------
    override suspend fun search(query: String): List<SearchResponse> {
        val all = fetchMovies("$mainUrl/api/movies?limit=2000&sort=release_date")
        return all.filter { it.name?.contains(query, ignoreCase = true) == true }
    }

    // ------------------------------------------------------------
    // Load movie details (uses cached poster/backdrop)
    // ------------------------------------------------------------
    override suspend fun load(url: String): LoadResponse {
        val movie = movieStore[url] ?: throw Error("Movie not found")
        return newMovieLoadResponse(movie.title, url, TvType.Movie, movie.streamUrl) {
            this.plot = movie.plot
            this.year = movie.year
            this.posterUrl = movie.poster
            this.backgroundPosterUrl = movie.backdrop
            val tagsList = mutableListOf<String>()
            tagsList.addAll(movie.genres)
            if (movie.cast.isNotEmpty()) {
                tagsList.add("Cast: ${movie.cast.joinToString(", ")}")
            }
            if (tagsList.isNotEmpty()) this.tags = tagsList
            // score is omitted to avoid type mismatch (Score? vs Double)
        }
    }

    // ------------------------------------------------------------
    // Video extraction – direct URL
    // ------------------------------------------------------------
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