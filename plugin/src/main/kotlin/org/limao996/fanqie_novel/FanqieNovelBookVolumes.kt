package org.limao996.fanqie_novel

import com.nfeld.jsonpathkt.kotlinx.resolvePathAsStringOrNull
import com.nfeld.jsonpathkt.kotlinx.resolvePathOrNull
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.book.BookVolumes
import io.nightfish.lightnovelreader.api.book.ChapterInformation
import io.nightfish.lightnovelreader.api.book.Volume
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.limao996.fanqie_novel.utils.get

suspend fun FanqieNovelBookVolumes(id: String): BookVolumes {
    val args = id.split(':')
    val json = get("$HOST/api/book?book_id=${args[1]}") ?: return BookVolumes.empty()

    val volumes = json.resolvePathOrNull("$.data.data.chapterListWithVolume")?.jsonArray
        ?: return BookVolumes.empty()
    val volumeNames = json.resolvePathOrNull("$.data.data.volumeNameList")?.jsonArray
        ?: return BookVolumes.empty()

    return BookVolumes(
        id, volumeNames.zip(volumes).mapIndexed { index, pair ->
            Volume(
                volumeId = index.toString(),
                volumeTitle = pair.first.jsonPrimitive.content,
                chapters = pair.second.jsonArray.map {
                    ChapterInformation(
                        id = it.resolvePathAsStringOrNull("$.itemId") ?: "",
                        title = it.resolvePathAsStringOrNull("$.title") ?: ""
                    )
                })
        })
}