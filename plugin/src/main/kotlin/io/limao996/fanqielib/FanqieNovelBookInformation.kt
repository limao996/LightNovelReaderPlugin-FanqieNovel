package io.limao996.fanqielib

import android.net.Uri
import androidx.core.net.toUri
import com.nfeld.jsonpathkt.kotlinx.resolvePathAsStringOrNull
import com.nfeld.jsonpathkt.kotlinx.resolvePathOrNull
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.book.MutableBookInformation
import io.nightfish.lightnovelreader.api.book.WordCount
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import io.limao996.fanqielib.utils.get
import io.limao996.fanqielib.utils.legacyToLocalDateTime

suspend fun FanqieNovelBookInformation(
    id: String
): BookInformation {
    val args = id.split(':')
    val json = get("$HOST/api/detail?book_id=${args[1]}") ?: return BookInformation.empty()
    val code = json.resolvePathOrNull("$.code")?.jsonPrimitive?.int
    if (code != 200) return BookInformation.empty()

    val data = json.resolvePathOrNull("$.data.data") ?: return BookInformation.empty(id)

    return MutableBookInformation(
        id = id,
        title = data.resolvePathAsStringOrNull("$.book_name") ?: return BookInformation.empty(id),
        subtitle = data.resolvePathAsStringOrNull("$.book_short_name")
            ?: data.resolvePathAsStringOrNull("$.original_book_name") ?: "",
        coverUrl = data.resolvePathAsStringOrNull("$.thumb_url")?.toUri() ?: Uri.EMPTY,
        author = data.resolvePathAsStringOrNull("$.author") ?: "",
        description = data.resolvePathAsStringOrNull("$.abstract") ?: "",
        tags = data.resolvePathAsStringOrNull("$.tags")?.split(',') ?: emptyList(),
        publishingHouse = data.resolvePathAsStringOrNull("$.source") ?: "番茄小说",
        wordCount = WordCount(
            data.resolvePathAsStringOrNull("$.word_number")?.toInt() ?: 0
        ),
        lastUpdated = legacyToLocalDateTime(
            data.resolvePathAsStringOrNull("$.last_publish_time")?.toLong() ?: 0
        ),
        isComplete = data.resolvePathAsStringOrNull("$.update_status") == "0"
    )
}