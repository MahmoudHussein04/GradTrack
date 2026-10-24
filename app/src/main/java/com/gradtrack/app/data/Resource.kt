package com.gradtrack.app.data

import com.google.gson.Gson
import com.gradtrack.app.data.model.ApiErrorBody
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

enum class ErrorKind {
    NO_CONNECTION,
    TIMEOUT,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    VALIDATION,
    SERVER,
    BAD_RESPONSE,
    UNKNOWN,
}

/** A failure the UI can describe to the user. [serverMessage] is the backend's own text when it sent one. */
data class AppError(val kind: ErrorKind, val serverMessage: String? = null)

/** Result of a repository call: either data or an [AppError]. Repositories never throw to ViewModels. */
sealed interface Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>

    data class Failure(val error: AppError) : Resource<Nothing>
}

private val errorGson = Gson()

/** Runs a network call and converts every failure into a [Resource.Failure]. Cancellation is rethrown. */
suspend fun <T> safeCall(block: suspend () -> T): Resource<T> =
    try {
        Resource.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Resource.Failure(e.toAppError())
    }

fun Throwable.toAppError(): AppError =
    when (this) {
        is HttpException -> {
            val message = runCatching { errorGson.fromJson(response()?.errorBody()?.string(), ApiErrorBody::class.java)?.message }.getOrNull()
            val kind =
                when (code()) {
                    400, 422 -> ErrorKind.VALIDATION
                    401 -> ErrorKind.UNAUTHORIZED
                    403 -> ErrorKind.FORBIDDEN
                    404 -> ErrorKind.NOT_FOUND
                    409 -> ErrorKind.CONFLICT
                    in 500..599 -> ErrorKind.SERVER
                    else -> ErrorKind.UNKNOWN
                }
            AppError(kind, message?.takeIf { it.isNotBlank() })
        }
        is SocketTimeoutException -> AppError(ErrorKind.TIMEOUT)
        is IOException -> AppError(ErrorKind.NO_CONNECTION)
        is com.google.gson.JsonParseException -> AppError(ErrorKind.BAD_RESPONSE)
        else -> AppError(ErrorKind.UNKNOWN)
    }
