package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URLEncoder

@Suppress("UNCHECKED_CAST")
class ExampleProvider : MainAPI() {
    override var name = "DhakaMovie BDIX"
    override var lang = "bn"
    override var mainUrl = "http://dhakamovie.com:8080"
    override val hasMainPage = true
    override val hasQuickSearch = true

    private val apiEndpoint = "$mainUrl/api/movies"
    private val mapper = jacksonObjectMapper()

    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Accept" to "application/json",
        "Referer" to mainUrl
    )

    override suspend fun search(query: String): List<SearchResponse> {
        val allMovies = mutableListOf<Map<String, Any>>()

        for (page in 1..3) {
            val url = "$apiEndpoint?page=$page"
            val response = app.get(url, headers = headers).text
            val json = mapper.readValue<Map<String, Any>>(response)
            val movies = json["data"] as? List<Map<String, Any>> ?: break
            allMovies.addAll(movies)
            if (movies.size < 12) break
        }

        val filtered = allMovies.filter { movie ->
            val title = movie["title"] as? String ?: ""
            title.contains(query, ignoreCase = true)
        }

        return filtered.mapNotNull { movie ->
            val id = movie["id"]?.toString() ?: return@mapNotNull null
            val title = movie["title"] as? String ?: return@mapNotNull null
            val poster = movie["poster_url"] as? String ?: ""
            val year = (movie["year"] as? String)?.toIntOrNull()
            val streamUrl = movie["stream_url"] as? String ?: ""
            val backdrop = movie["backdrop_url"] as? String ?: ""
            val plot = movie["overview"] as? String ?: ""
            val rating = (movie["rating"] as? String)?.toDoubleOrNull()
            val duration = (movie["runtime"] as? String)?.toIntOrNull()
            val director = movie["director"] as? String ?: ""
            val genresStr = movie["genres"] as? String ?: ""

            // Encode all data into the URL so load() can use it
            val encodedData = URLEncoder.encode(
                "$title|$streamUrl|$poster|$backdrop|$plot|$year|$rating|$duration|$director|$genresStr",
                "UTF-8"
            )

            newMovieSearchResponse(title, "data:$encodedData", TvType.Movie, false) {
                this.posterUrl = poster
                this.year = year
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        // Extract encoded data from "data:..." URL
        val encodedData = url.removePrefix("data:")
        val decoded = java.net.URLDecoder.decode(encodedData, "UTF-8")
        val parts = decoded.split("|")

        val title = parts.getOrNull(0) ?: throw Error("No title")
        val streamUrl = parts.getOrNull(1) ?: ""
        val poster = parts.getOrNull(2) ?: ""
        val backdrop = parts.getOrNull(3) ?: ""
        val plot = parts.getOrNull(4) ?: ""
        val year = parts.getOrNull(5)?.toIntOrNull()
        val rating = parts.getOrNull(6)?.toDoubleOrNull()
        val duration = parts.getOrNull(7)?.toIntOrNull()
        val director = parts.getOrNull(8) ?: ""
        val genresStr = parts.getOrNull(9) ?: ""
        val genres = genresStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        return newMovieLoadResponse(title, streamUrl, TvType.Movie, streamUrl) {
            this.plot = plot
            this.year = year
            this.posterUrl = poster
            this.backgroundPosterUrl = backdrop
            this.duration = duration
            val tagsList = mutableListOf<String>()
            if (director.isNotBlank()) tagsList.add("Director: $director")
            tagsList.addAll(genres)
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

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = "$apiEndpoint?page=$page"
        val response = app.get(url, headers = headers).text
        val json = mapper.readValue<Map<String, Any>>(response)
        val movies = json["data"] as? List<Map<String, Any>> ?: return newHomePageResponse(listOf())

        val list = movies.mapNotNull { movie ->
            val id = movie["id"]?.toString() ?: return@mapNotNull null
            val title = movie["title"] as? String ?: return@mapNotNull null
            val poster = movie["poster_url"] as? String ?: ""
            val streamUrl = movie["stream_url"] as? String ?: ""
            val backdrop = movie["backdrop_url"] as? String ?: ""
            val plot = movie["overview"] as? String ?: ""
            val year = (movie["year"] as? String)?.toIntOrNull()
            val rating = (movie["rating"] as? String)?.toDoubleOrNull()
            val duration = (movie["runtime"] as? String)?.toIntOrNull()
            val director = movie["director"] as? String ?: ""
            val genresStr = movie["genres"] as? String ?: ""

            val encodedData = URLEncoder.encode(
                "$title|$streamUrl|$poster|$backdrop|$plot|$year|$rating|$duration|$director|$genresStr",
                "UTF-8"
            )

            newMovieSearchResponse(title, "data:$encodedData", TvType.Movie, false) {
                this.posterUrl = poster
                this.year = year
            }
        }

        return newHomePageResponse(listOf(HomePageList("Latest Movies", list)))
    }
}