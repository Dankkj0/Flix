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
    override var mainUrl = "http://dhakamovie.com"
    override val hasMainPage = true
    override val hasQuickSearch = true

    private val apiMoviesBase = "http://dhakamovie.com:8080/api/movies"
    private val apiTvSeriesBase = "http://dhakamovie.com:8080/api/tv-series"
    private val advancedSearchBase = "http://dhakamovie.com:8080/api/advanced-search"
    private val mapper = jacksonObjectMapper()

    companion object {
        val movieStore = mutableMapOf<String, MovieData>()
        val episodeStore = mutableMapOf<String, String>()
        val seriesStore = mutableMapOf<String, SeriesData>()
    }

    data class MovieData(
        val title: String,
        val streamUrl: String,
        val poster: String,
        val backdrop: String,
        val plot: String,
        val year: Int?,
        val rating: Double?,
        val duration: Int?,
        val director: String,
        val genres: List<String>
    )

    data class EpisodeData(
        val title: String,
        val episodeNumber: Int,
        val filePath: String,
        val runtime: Int?,
        val poster: String
    )

    data class SeasonData(
        val seasonNumber: Int,
        val episodes: List<EpisodeData>
    )

    data class SeriesData(
        val title: String,
        val poster: String,
        val backdrop: String,
        val plot: String,
        val year: Int?,
        val rating: Double?,
        val genres: List<String>,
        val seasons: List<SeasonData>
    )

    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Accept" to "application/json",
        "Referer" to mainUrl
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val lists = mutableListOf<HomePageList>()

        // All Movies (advanced search, up to 1000)
        val allMovies = fetchMovies("$advancedSearchBase?query=&type=movies&page=1&per_page=1000&order_by=Latest")
        if (allMovies.isNotEmpty()) lists.add(HomePageList("All Movies (1000+)", allMovies))

        // TV Series (advanced search, up to 1000)
        val tvSeries = fetchSeries("$advancedSearchBase?query=&type=tv_series&page=1&per_page=1000&order_by=Latest")
        if (tvSeries.isNotEmpty()) lists.add(HomePageList("TV Series (1000+)", tvSeries))

        // Latest Movies
        val latest = fetchMovies("$apiMoviesBase/latest")
        if (latest.isNotEmpty()) lists.add(HomePageList("Latest Movies", latest))

        // New Releases
        val newReleases = fetchMovies("$apiMoviesBase/new-releases")
        if (newReleases.isNotEmpty()) lists.add(HomePageList("New Releases", newReleases))

        // Trending
        val trending = fetchMovies("$apiMoviesBase/trending")
        if (trending.isNotEmpty()) lists.add(HomePageList("Trending", trending))

        // Top 10
        val top10 = fetchMovies("$apiMoviesBase/top-10")
        if (top10.isNotEmpty()) lists.add(HomePageList("Top 10", top10))

        // South Indian Movies
        val southIndian = fetchMovies("$advancedSearchBase?query=&type=movies&page=1&per_page=1000&category=South%20Indian&order_by=Latest")
        if (southIndian.isNotEmpty()) lists.add(HomePageList("South Indian Movies", southIndian))

        // Korean TV Series
        val tvSeriesKor = fetchSeries("$advancedSearchBase?query=&type=tv_series&page=1&per_page=1000&category=Korean&order_by=Latest")
        if (tvSeriesKor.isNotEmpty()) lists.add(HomePageList("Korean TV Series", tvSeriesKor))

        // Netflix Movies
        val movieNetflix = fetchMovies("$advancedSearchBase?query=&type=movies&page=1&per_page=1000&category=Netflix&order_by=Latest")
        if (movieNetflix.isNotEmpty()) lists.add(HomePageList("NetFlix Movies", movieNetflix))

        // Netflix TV Series
        val tvSeriesNetflix = fetchSeries("$advancedSearchBase?query=&type=tv_series&page=1&per_page=1000&category=Netflix&order_by=Latest")
        if (tvSeriesNetflix.isNotEmpty()) lists.add(HomePageList("NetFlix TV Series", tvSeriesNetflix))

        // Prime Movies
        val moviePrime = fetchMovies("$advancedSearchBase?query=&type=movies&page=1&per_page=1000&category=Prime&order_by=Latest")
        if (moviePrime.isNotEmpty()) lists.add(HomePageList("Prime Movies", moviePrime))

        // Prime TV Series
        val tvSeriesPrime = fetchSeries("$advancedSearchBase?query=&type=tv_series&page=1&per_page=1000&category=Prime&order_by=Latest")
        if (tvSeriesPrime.isNotEmpty()) lists.add(HomePageList("Prime TV Series", tvSeriesPrime))

        // Hindi Movies
        val hindiMovie = fetchMovies("$advancedSearchBase?query=&type=movies&page=1&per_page=1000&category=Bollywood&order_by=Latest")
        if (hindiMovie.isNotEmpty()) lists.add(HomePageList("Hindi Movies", hindiMovie))

        // Hindi TV Series
        val tvSeriesHindi = fetchSeries("$advancedSearchBase?query=&type=tv_series&page=1&per_page=1000&category=Hindi&order_by=Latest")
        if (tvSeriesHindi.isNotEmpty()) lists.add(HomePageList("Hindi TV Series", tvSeriesHindi))

        // Hollywood Movies
        val hollywoodMovie = fetchMovies("$advancedSearchBase?query=&type=movies&page=1&per_page=1000&category=Hollywood&order_by=Latest")
        if (hollywoodMovie.isNotEmpty()) lists.add(HomePageList("Hollywood Movies", hollywoodMovie))

        // Hollywood TV Series
        val tvSeriesHollywood = fetchSeries("$advancedSearchBase?query=&type=tv_series&page=1&per_page=1000&category=English&order_by=Latest")
        if (tvSeriesHollywood.isNotEmpty()) lists.add(HomePageList("Hollywood TV Series", tvSeriesHollywood))

        // Indian Bangla Movies
        val IndianBanglaMovie = fetchMovies("$advancedSearchBase?query=&type=movies&page=1&per_page=1000&category=Indian+Bangla&order_by=Latest")
        if (IndianBanglaMovie.isNotEmpty()) lists.add(HomePageList("Indian Bangla Movies", IndianBanglaMovie))

        // Indian Bangla TV Series
        val tvSeriesIndianBangla = fetchSeries("$advancedSearchBase?query=&type=tv_series&page=1&per_page=1000&category=Indian+Bangla&order_by=Latest")
        if (tvSeriesIndianBangla.isNotEmpty()) lists.add(HomePageList("Indian Bangla TV Series", tvSeriesIndianBangla))

        return newHomePageResponse(lists)
    }

    // Universal movie fetcher (returns fake detail URLs)
    private suspend fun fetchMovies(apiUrl: String): List<SearchResponse> {
        return try {
            val response = app.get(apiUrl, headers = headers).text
            val json = mapper.readValue<Map<String, Any>>(response)

            val movies = when {
                json.containsKey("results") -> {
                    val results = json["results"] as? Map<String, Any>
                    val moviesObj = results?.get("movies") as? Map<String, Any>
                    moviesObj?.get("data") as? List<Map<String, Any>> ?: emptyList()
                }
                json.containsKey("data") -> {
                    json["data"] as? List<Map<String, Any>> ?: emptyList()
                }
                else -> emptyList()
            }

            movies.mapNotNull { movie ->
                val slug = movie["slug"] as? String ?: return@mapNotNull null
                val title = movie["title"] as? String ?: return@mapNotNull null
                val poster = movie["poster_url"] as? String ?: movie["image"] as? String ?: ""
                val fullPoster = if (poster.startsWith("/")) "$mainUrl:8080$poster" else poster
                val year = (movie["year"] as? String)?.toIntOrNull()
                val streamUrl = movie["stream_url"] as? String ?: ""
                val backdrop = movie["backdrop_url"] as? String ?: fullPoster
                val plot = movie["overview"] as? String ?: "No plot available"
                val rating = (movie["rating"] as? String)?.toDoubleOrNull()
                val duration = (movie["runtime"] as? String)?.toIntOrNull()
                val director = movie["director"] as? String ?: "Unknown"
                val genresStr = movie["genres"] as? String ?: ""
                val genres = genresStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                val fakeUrl = "http://movie.local/$slug"
                movieStore[fakeUrl] = MovieData(
                    title, streamUrl, fullPoster, backdrop, plot,
                    year, rating, duration, director, genres
                )

                newMovieSearchResponse(title, fakeUrl, TvType.Movie, false) {
                    this.posterUrl = fullPoster
                    this.year = year
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // TV series fetcher (returns fake detail URLs)
    private suspend fun fetchSeries(apiUrl: String): List<SearchResponse> {
        return try {
            val response = app.get(apiUrl, headers = headers).text
            val json = mapper.readValue<Map<String, Any>>(response)
            val results = json["results"] as? Map<String, Any> ?: return emptyList()
            val seriesObj = results["series"] as? Map<String, Any> ?: return emptyList()
            val seriesList = seriesObj["data"] as? List<Map<String, Any>> ?: return emptyList()

            seriesList.mapNotNull { series ->
                val slug = series["slug"] as? String ?: return@mapNotNull null
                val title = series["title"] as? String ?: return@mapNotNull null
                val poster = series["image"] as? String ?: ""
                val fullPoster = if (poster.startsWith("/")) "$mainUrl:8080$poster" else poster
                val year = (series["year"] as? String)?.toIntOrNull()

                val fakeUrl = "http://tv.local/$slug"
                seriesStore[fakeUrl] = SeriesData(
                    title, fullPoster, fullPoster, "", year, null, emptyList(), emptyList()
                )

                newTvSeriesSearchResponse(title, fakeUrl, TvType.TvSeries) {
                    this.posterUrl = fullPoster
                    this.year = year
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Fetch full series details – with URL‑encoded slug
    private suspend fun fetchFullSeries(slug: String): SeriesData? {
        val encodedSlug = URLEncoder.encode(slug, "UTF-8")
        val url = "$apiTvSeriesBase/$encodedSlug"
        return try {
            val response = app.get(url, headers = headers).text
            val series = mapper.readValue<Map<String, Any>>(response)

            val title = series["title"] as? String ?: return null
            val poster = series["poster_url"] as? String ?: ""
            val fullPoster = if (poster.startsWith("/")) "$mainUrl:8080$poster" else poster
            val year = (series["year"] as? String)?.toIntOrNull()
            val plot = series["overview"] as? String ?: "No plot available"
            val rating = when (val r = series["rating"]) {
                is Number -> r.toDouble()
                is String -> r.toDoubleOrNull()
                else -> null
            }
            val genresStr = series["genres"] as? String ?: ""
            val genres = genresStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val backdrop = (series["property"] as? Map<String, Any>)?.get("backdrop_path") as? String ?: fullPoster
            val fullBackdrop = if (backdrop.startsWith("/")) "$mainUrl:8080$backdrop" else backdrop

            val seasonsList = mutableListOf<SeasonData>()
            val seasonsRaw = series["seasons"] as? List<Map<String, Any>> ?: emptyList()
            for (seasonRaw in seasonsRaw) {
                val seasonNumber = seasonRaw["season_number"] as? Int ?: continue
                val episodesRaw = seasonRaw["episodes"] as? List<Map<String, Any>> ?: continue
                val episodes = episodesRaw.mapNotNull { ep ->
                    val epNum = ep["episode_number"] as? Int ?: return@mapNotNull null
                    val epTitle = ep["title"] as? String ?: "Episode $epNum"
                    val property = ep["property"] as? Map<String, Any>
                    val rawPath = property?.get("file_path") as? String
                    if (rawPath.isNullOrEmpty()) return@mapNotNull null
                    val cleanedPath = rawPath.removePrefix("server1/")
                    val runtime = property?.get("runtime") as? Int
                    val epPoster = ep["poster_url"] as? String ?: ""
                    val fullEpPoster = if (epPoster.startsWith("/")) "$mainUrl:8080$epPoster" else epPoster
                    EpisodeData(epTitle, epNum, cleanedPath, runtime, fullEpPoster)
                }
                if (episodes.isNotEmpty()) {
                    seasonsList.add(SeasonData(seasonNumber, episodes))
                }
            }

            SeriesData(title, fullPoster, fullBackdrop, plot, year, rating, genres, seasonsList)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val allMovies = mutableListOf<SearchResponse>()
        for (page in 1..2) {
            val results = fetchMovies("$apiMoviesBase?page=$page")
            allMovies.addAll(results)
            if (results.size < 12) break
        }
        return allMovies.filter { it.name?.contains(query, ignoreCase = true) == true }
    }

    override suspend fun load(url: String): LoadResponse {
        // If a direct video URL somehow reaches here, throw error to debug
        if (url.startsWith("http://server1.dhakamovie.com")) {
            throw Error("Direct video URL passed to load() – bug. URL: $url")
        }
        if (url.startsWith("http://tv.local/")) {
            val slug = url.removePrefix("http://tv.local/")
            val series = fetchFullSeries(slug) ?: throw Error("Series not found: $slug")
            val seriesUrl = "http://tv.local/${series.title}"
            seriesStore[seriesUrl] = series

            val episodes = mutableListOf<Episode>()
            for (seasonData in series.seasons) {
                for (ep in seasonData.episodes) {
                    val episodeUrl = "http://episode.local/${slug}/${seasonData.seasonNumber}/${ep.episodeNumber}"
                    episodes.add(
                        newEpisode(episodeUrl) {
                            name = ep.title
                            season = seasonData.seasonNumber
                            episode = ep.episodeNumber
                            posterUrl = ep.poster
                            runTime = ep.runtime
                        }
                    )
                    episodeStore[episodeUrl] = ep.filePath
                }
            }
            return newTvSeriesLoadResponse(series.title, seriesUrl, TvType.TvSeries, episodes) {
                this.plot = series.plot
                this.year = series.year
                this.posterUrl = series.poster
                this.backgroundPosterUrl = series.backdrop
                if (series.genres.isNotEmpty()) this.tags = series.genres
            }
        }
        if (movieStore.containsKey(url)) {
            val movie = movieStore[url]!!
            return newMovieLoadResponse(movie.title, movie.streamUrl, TvType.Movie, movie.streamUrl) {
                this.plot = movie.plot
                this.year = movie.year
                this.posterUrl = movie.poster
                this.backgroundPosterUrl = movie.backdrop
                this.duration = movie.duration
                val tags = mutableListOf<String>()
                if (movie.director.isNotBlank()) tags.add("Director: ${movie.director}")
                tags.addAll(movie.genres)
                if (tags.isNotEmpty()) this.tags = tags
            }
        }
        throw Error("Unknown URL: $url")
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if (data.startsWith("http://episode.local/")) {
            val filePath = episodeStore[data] ?: return false
            val streamUrl = "http://server1.dhakamovie.com/$filePath"
            val encodedUrl = streamUrl.replace(" ", "%20")
            val quality = if (encodedUrl.contains("1080")) 1080 else if (encodedUrl.contains("720")) 720 else 0
            callback.invoke(
                newExtractorLink(source = name, name = "Direct", url = encodedUrl) {
                    this.referer = mainUrl
                    this.quality = quality
                }
            )
            return true
        }
        val quality = when {
            data.contains("1080") -> 1080
            data.contains("720") -> 720
            data.contains("480") -> 480
            else -> 0
        }
        callback.invoke(
            newExtractorLink(source = name, name = "Direct", url = data) {
                this.referer = mainUrl
                this.quality = quality
            }
        )
        return true
    }
}