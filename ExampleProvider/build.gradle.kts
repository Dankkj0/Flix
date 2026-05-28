import com.lagradost.cloudstream3.gradle.CloudstreamExtension

val pluginConfig = extensions.getByType<CloudstreamExtension>()

pluginConfig.apply {
    pluginId = "StreamBDPlugin"
    pluginName = "StreamBD IPTV"
    // এখানে প্যাকেজের নাম ঠিক করে দেওয়া হয়েছে
    pluginClass = "com.iptv.streambd.StreamBDPlugin" 
    pluginVersion = 1
    description = "StreamBD IPTV Extension with Token Parser"
}
