package com.example

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class ExamplePlugin: Plugin() {
    override fun load(context: Context) {
        // Isso avisa ao Cloudstream para carregar o seu raspador do PobreFlix
        registerMainAPI(PobreFlixProvider())
    }
}
