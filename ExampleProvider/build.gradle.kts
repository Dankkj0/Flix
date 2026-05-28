import com.lagradost.cloudstream3.gradle.CloudstreamExtension

val pluginConfig = extensions.getByType<CloudstreamExtension>()

pluginConfig.apply {
    this.pluginId = "StreamBDPlugin"
    this.pluginName = "StreamBD IPTV"
    this.pluginClass = "com.iptv.streambd.StreamBDPlugin" 
    this.pluginVersion = 1
    this.description = "StreamBD IPTV Extension with Token Parser"
}
