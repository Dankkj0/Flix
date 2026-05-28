package com.iptv.streambd

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

class StreamBDProvider : MainAPI() {
    override var mainUrl = "https://streambd-iptv.netlify.app/playlists/main.json"
    override var name = "StreamBD IPTV"
    override val hasMainPage = true
    override var lang = "bn"
    override val supportedTypes = setOf(TvType.Live)

    // লেটেস্ট ডকুমেন্টেশন অনুযায়ী মেইন পেইজ লোড করার বেসিক ফাংশন
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val list = emptyList<HomePageList>()
        return newHomePageResponse(list, false)
    }

    // লাইভ স্ট্রিম লোড করার নতুন বিল্ডার ফাংশন
    override suspend fun load(url: String): LoadResponse? {
        return newLiveStreamLoadResponse("Live Stream", url, url)
    }

    // ভিডিও লিংক প্লেয়ারে পাঠানোর লেটেস্ট নিয়ম
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        callback.invoke(
            ExtractorLink(
                source = this.name,
                name = "Live TV",
                url = data,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = data.contains(".m3u8", ignoreCase = true)
            )
        )
        return true
    }
}
