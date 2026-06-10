package com.korvus.nomnom.api

import java.io.IOException

class RotatableHttpException(val code: Int, val bodyExcerpt: String) :
    IOException("HTTP $code: $bodyExcerpt")

/**
 * Round-robin по списку ключей. На 401/402/403/429 двигает указатель и ретраит.
 * keys пустой = одна попытка без Authorization.
 */
suspend fun <T> rotateKeys(
    keys: List<String>,
    startIdx: Int,
    onAdvance: suspend () -> Unit,
    block: suspend (apiKey: String) -> T,
): T {
    val total = if (keys.isEmpty()) 1 else keys.size
    var lastErr: Throwable? = null
    val start = if (keys.isEmpty()) 0 else ((startIdx % keys.size) + keys.size) % keys.size
    for (i in 0 until total) {
        val key = if (keys.isEmpty()) "" else keys[(start + i) % keys.size]
        try {
            return block(key)
        } catch (e: RotatableHttpException) {
            if (e.code in setOf(401, 402, 403, 429)) {
                onAdvance()
                lastErr = e
                continue
            }
            throw e
        }
    }
    throw lastErr ?: IOException("All keys exhausted")
}
