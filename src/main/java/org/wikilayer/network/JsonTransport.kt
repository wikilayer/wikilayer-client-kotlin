package org.wikilayer.network

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class JsonTransport(
    private val client: OkHttpClient,
    private val mapper: ObjectMapper,
) {
    suspend fun <T> value(
        type: Class<T>,
        request: Request,
    ): T {
        val wholeBodyBeforeAnyParsing = data(request)
        try {
            return mapper.readValue(wholeBodyBeforeAnyParsing, type)
        } catch (everyComplaintTheParserMakes: IOException) {
            throw WikiApiError.Malformed(everyComplaintTheParserMakes)
        }
    }

    suspend fun data(request: Request): ByteArray =
        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { answer ->
                if (answer.code !in SUCCESS) throw WikiApiError.Status(answer.code)
                answer.body.bytes()
            }
        }

    private companion object {
        val SUCCESS = 200..299
    }
}
