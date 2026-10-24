package com.gradtrack.app.data

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class ResourceTest {
    private fun http(code: Int, body: String = "") = HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    @Test
    fun `success is wrapped`() =
        runTest {
            assertThat(safeCall { 42 }).isEqualTo(Resource.Success(42))
        }

    @Test
    fun `io errors mean no connection, timeouts are separate`() =
        runTest {
            assertThat((safeCall { throw IOException() } as Resource.Failure).error.kind).isEqualTo(ErrorKind.NO_CONNECTION)
            assertThat((safeCall { throw SocketTimeoutException() } as Resource.Failure).error.kind).isEqualTo(ErrorKind.TIMEOUT)
        }

    @Test
    fun `http status maps to a kind and keeps the server message`() =
        runTest {
            val forbidden = (safeCall { throw http(403, """{"message":"Only the leader can do this."}""") } as Resource.Failure).error
            assertThat(forbidden.kind).isEqualTo(ErrorKind.FORBIDDEN)
            assertThat(forbidden.serverMessage).isEqualTo("Only the leader can do this.")

            assertThat((safeCall { throw http(401) } as Resource.Failure).error.kind).isEqualTo(ErrorKind.UNAUTHORIZED)
            assertThat((safeCall { throw http(422) } as Resource.Failure).error.kind).isEqualTo(ErrorKind.VALIDATION)
            assertThat((safeCall { throw http(503, "<html>") } as Resource.Failure).error.kind).isEqualTo(ErrorKind.SERVER)
        }

    @Test
    fun `cancellation is never swallowed`() =
        runTest {
            assertThrows(CancellationException::class.java) { kotlinx.coroutines.runBlocking { safeCall { throw CancellationException("stop") } } }
        }
}
