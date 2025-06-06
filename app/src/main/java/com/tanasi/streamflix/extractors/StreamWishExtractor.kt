package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.JsUnpacker
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

open class StreamWishExtractor : Extractor() {

    override val name = "Streamwish"
    override val mainUrl = "https://streamwish.to"

    protected var referer = ""

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        val document = service.get(getEmbedUrl(link), referer = referer)


        val script = Regex("<script .*>(eval.*?)</script>", RegexOption.DOT_MATCHES_ALL).find(document.toString())
            ?.groupValues?.get(1)
            ?.let { JsUnpacker(it).unpack() }
            ?: throw Exception("Can't retrieve script")

        val source = Regex("\"hls\\d+\":\\s*\"(.*?m3u8.*?)\"").findAll(script)
            .map { it.groupValues.getOrNull(1) }
            .firstOrNull()
            ?: throw Exception("Can't retrieve m3u8")

        val subtitles = Regex("file:\\s*\"(.*?)\"(?:,label:\\s*\"(.*?)\")?,kind:\\s*\"(.*?)\"").findAll(
            Regex("tracks:\\s*\\[(.*?)]").find(script)
                ?.groupValues?.get(1)
                ?: ""
        )
            .filter { it.groupValues[3] == "captions" }
            .map {
                Video.Subtitle(
                    label = it.groupValues[2],
                    file = it.groupValues[1],
                )
            }
            .toList()

        val video = Video(
            source = source,
            subtitles = subtitles,
        )

        return video
    }

    private fun getEmbedUrl(url: String): String {
        return if (url.contains("/f/")) {
            val videoId = url.substringAfter("/f/")
            "$mainUrl/$videoId"
        } else if (url.contains("/e/")){
            val videoId = url.substringAfter("/e/")
            "$mainUrl/$videoId"

        } else {
            url
        }
    }


    class UqloadsXyz : StreamWishExtractor() {
        override val name = "Uqloads"
        override val mainUrl = "https://uqloads.xyz"

        suspend fun extract(link: String, referer: String): Video {
            this.referer = referer
            return extract(link)
        }
    }
    class SwiftPlayersExtractor : StreamWishExtractor(){
        override val name = "SwiftPlayer"
        override val mainUrl = "https://swiftplayers.com/"
    }

    class SwishExtractor : StreamWishExtractor() {
        override val name = "Swish"
        override val mainUrl = "https://swishsrv.com/"
    }

    class HlswishExtractor : StreamWishExtractor() {
        override val name = "Hlswish"
        override val mainUrl = "https://hlswish.com/"
    }

    class PlayerwishExtractor : StreamWishExtractor() {
        override val name = "Playerwish"
        override val mainUrl = "https://playerwish.com/"
    }


    private interface Service {

        companion object {
            fun build(baseUrl: String): Service {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .build()

                return retrofit.create(Service::class.java)
            }
        }

        @GET
        suspend fun get(
            @Url url: String,
            @Header("referer") referer: String = "",
        ): Document
    }
}