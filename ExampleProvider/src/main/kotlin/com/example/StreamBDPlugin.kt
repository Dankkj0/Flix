package com.iptv.streambd

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class StreamBDPlugin: Plugin() {
    override fun load(context: Context) {
        // প্রোভাইডার রেজিস্টার করা হচ্ছে
        registerMainAPI(StreamBDProvider())
    }
}
