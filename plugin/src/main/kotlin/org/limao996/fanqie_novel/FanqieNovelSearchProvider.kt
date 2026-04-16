package org.limao996.fanqie_novel

import android.net.Uri
import androidx.core.net.toUri
import com.nfeld.jsonpathkt.kotlinx.resolvePathAsStringOrNull
import com.nfeld.jsonpathkt.kotlinx.resolvePathOrNull
import io.nightfish.lightnovelreader.api.book.MutableBookInformation
import io.nightfish.lightnovelreader.api.book.WordCount
import io.nightfish.lightnovelreader.api.util.local
import io.nightfish.lightnovelreader.api.web.search.SearchProvider
import io.nightfish.lightnovelreader.api.web.search.SearchResult
import io.nightfish.lightnovelreader.api.web.search.SearchType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.limao996.fanqie_novel.utils.get
import org.limao996.fanqie_novel.utils.legacyToLocalDateTime
import java.net.URLEncoder
import kotlin.time.Duration.Companion.seconds

object FanqieNovelSearchProvider : SearchProvider {
    override val searchTypes: List<SearchType> = listOf(
        SearchType("3", "搜索小说".local(), "请输入小说名称".local()),
        SearchType("8", "搜索漫画".local(), "请输入漫画名称".local()),
    )

    override fun search(
        searchType: SearchType, keyword: String
    ): Flow<SearchResult> = flow {
        val q = URLEncoder.encode(keyword, "utf-8")
        var offset = 0
        while (currentCoroutineContext().isActive) {
            val json = get("$HOST/api/search?key=$q&tab_type=${searchType.type}&offset=$offset")
            if (json == null) {
                emit(SearchResult.Error("API请求失败！"))
                return@flow
            }
            val code = json.resolvePathOrNull("$.code")?.jsonPrimitive?.int
            if (code != 200) {
                emit(SearchResult.Error("API请求失败！Code $code"))
                return@flow
            }

            val items =
                json.resolvePathOrNull("$.data.search_tabs[${if (searchType.type == "3") "5" else "3"}]")
                    ?: continue

            items.resolvePathOrNull("$.data")?.jsonArray?.forEach {
                val data = it.resolvePathOrNull("$.book_data[0]")?.jsonObject ?: return@forEach
                emit(
                    SearchResult.MultipleBook(
                        MutableBookInformation(
                            id = searchType.type + ":" + (data.resolvePathAsStringOrNull("$.book_id")
                                ?: return@forEach),
                            title = data.resolvePathAsStringOrNull("$.book_name") ?: return@forEach,
                            subtitle = data.resolvePathAsStringOrNull("$.book_short_name")
                                ?: data.resolvePathAsStringOrNull("$.original_book_name") ?: "",
                            coverUrl = data.resolvePathAsStringOrNull("$.thumb_url")?.toUri()
                                ?: Uri.EMPTY,
                            author = data.resolvePathAsStringOrNull("$.author") ?: "",
                            description = data.resolvePathAsStringOrNull("$.abstract") ?: "",
                            tags = data.resolvePathAsStringOrNull("$.tags")?.split(',')
                                ?: emptyList(),
                            publishingHouse = data.resolvePathAsStringOrNull("$.source")
                                ?: "番茄小说",
                            wordCount = WordCount(
                                data.resolvePathAsStringOrNull("$.word_number")?.toInt() ?: 0
                            ),
                            lastUpdated = legacyToLocalDateTime(
                                data.resolvePathAsStringOrNull("$.last_publish_time")?.toLong() ?: 0
                            ),
                            isComplete = data.resolvePathAsStringOrNull("$.creation_status") == "0"
                        )
                    )
                )
            }

            if (items.resolvePathOrNull("$.has_more")?.jsonPrimitive?.boolean == false) break
            offset = items.resolvePathOrNull("$.next_offset")?.jsonPrimitive?.int ?: offset

            delay(5.seconds)
        }
        emit(SearchResult.End())
    }.flowOn(Dispatchers.IO)
}