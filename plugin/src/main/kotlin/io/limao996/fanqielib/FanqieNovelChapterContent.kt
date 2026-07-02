package io.limao996.fanqielib

import androidx.core.net.toUri
import com.nfeld.jsonpathkt.kotlinx.resolvePathAsStringOrNull
import io.nightfish.lightnovelreader.api.book.ChapterContent
import io.nightfish.lightnovelreader.api.book.LocalBookDataSourceApi
import io.nightfish.lightnovelreader.api.book.MutableChapterContent
import io.nightfish.lightnovelreader.api.content.builder.ContentBuilder
import io.nightfish.lightnovelreader.api.content.builder.image
import io.nightfish.lightnovelreader.api.content.builder.simpleText
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import io.limao996.fanqielib.utils.get

suspend fun FanqieNovelChapterContent(
    chapterId: String, bookId: String,
    localBookDataSourceApi: LocalBookDataSourceApi,
): ChapterContent {
    val args = bookId.split(':')
    val json =
        get("$HOST/api/content?item_id=$chapterId&tab=" + if (args[0] == "3") "小说" else "漫画")
            ?: return MutableChapterContent.empty()

    val volumes = localBookDataSourceApi.getBookVolumes(bookId)!!.volumes
    val flatChapter = volumes.flatMap { volume -> volume.chapters }
    val flatChapterIds = flatChapter.map { it.id }
    val currentIndex = flatChapterIds.indexOf(chapterId)
    val prevId = flatChapterIds.getOrNull(currentIndex - 1)
    val nextId = flatChapterIds.getOrNull(currentIndex + 1)

    return MutableChapterContent(
        id = chapterId,
        title = flatChapter.getOrNull(currentIndex)?.title ?: "正文",
        content = ContentBuilder().apply {
            val buffer = ArrayList<String>()
            if (args[0] == "3") {
                for (line in (json.resolvePathAsStringOrNull("$.data.content")
                    ?: return@apply).lineSequence()) {
                    if (line.contains("<img")) {
                        if (buffer.isNotEmpty()) {
                            simpleText(buffer.joinToString("\n\n"))
                            buffer.clear()
                        }
                        extractImgSrc(line.trim())?.let { image(it.toUri()) }
                        continue
                    }

                    cleanHtml(line).trim().split("\n").filter { it.isNotBlank() }
                        .also { if (it.isEmpty()) break }.joinToString("\n\n") {
                            "ㅤㅤ${it.trim()}"
                        }.let(buffer::add)
                }
                if (buffer.isNotEmpty()) {
                    simpleText(buffer.joinToString("\n\n"))
                    buffer.clear()
                }
            } else {
                "src=\"([^\"]*)\"".toRegex()
                    .findAll(json.resolvePathAsStringOrNull("$.data.images")!!).forEach {
                        image(it.groupValues[1].toUri())
                    }
            }
        }.build(),
        lastChapter = prevId ?: "",
        nextChapter = nextId ?: ""
    )
}

private fun cleanHtml(html: String): String {
    return Jsoup.clean(html, Safelist.none().addTags("img")).replace(Regex("\\[\\d+[a-z]]"), "")
}

private fun extractImgSrc(html: String): String? {
    val pattern = Regex("<img[^>]+src=\"([^\"]+)\"", RegexOption.IGNORE_CASE)
    val matchResult = pattern.find(html)
    return matchResult?.groupValues?.get(1)?.let {
        var result = it.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")

        // 处理数字实体 &#xx; 和 &#xXX;
        val numericPattern = Regex("&#(x?[0-9A-Fa-f]+);")
        result = numericPattern.replace(result) { matchResult ->
            val code = matchResult.groupValues[1]
            val charCode = if (code.startsWith("x", ignoreCase = true)) {
                code.substring(1).toInt(16)
            } else {
                code.toInt()
            }
            charCode.toChar().toString()
        }

        result
    }
}
