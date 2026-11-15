package com.nonton.app.network

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import retrofit2.Response

/**
 * Runs a Retrofit request and converts both transport and HTTP errors into a
 * [Result]. The response body is returned on success, so callers never need to
 * unwrap a Retrofit [Response] themselves.
 */
suspend fun <T> mapToResult(block: suspend () -> Response<T>): Result<T> = try {
    val response = block()
    if (!response.isSuccessful) {
        val details = response.errorBody()?.string().orEmpty()
        val message = buildString {
            append("HTTP ${response.code()}")
            if (details.isNotBlank()) {
                append(": ")
                append(details)
            }
        }
        Result.failure(ApiException(response.code(), message))
    } else {
        response.body()?.let { body ->
            Result.success(body)
        } ?: Result.failure(IOException("Response body is empty"))
    }
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (timeout: SocketTimeoutException) {
    Result.failure(IOException("Request timed out", timeout))
} catch (unknownHost: UnknownHostException) {
    Result.failure(IOException("No internet connection", unknownHost))
} catch (io: IOException) {
    Result.failure(io)
} catch (error: Exception) {
    Result.failure(IOException(error.message ?: "Unexpected error", error))
}

class ApiException(
    val statusCode: Int,
    message: String
) : IOException(message)
