package io.limao996.fanqielib.utils

import cxhttp.CxHttp
import cxhttp.response.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private val requestLimiter = Semaphore(3)

suspend fun get(url: String): JsonElement? = withContext(Dispatchers.IO) {
    requestLimiter.withPermit {
        suspend fun get(): Response {
            return CxHttp.get(url).scope(this).await()
        }

        var retryTime = 3
        var retryDelay = 2500L
        var response = get()
        while (!response.isSuccessful && retryTime >= 1) {
            response = get()
            retryTime--
            delay(retryDelay)
            retryDelay *= 2
        }
        val json = response.body?.string()?.let(Json::parseToJsonElement)
        return@withContext json
    }
}

