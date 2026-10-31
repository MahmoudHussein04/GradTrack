package com.gradtrack.app.data.local

import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

/** The saved app state and the version it was saved at. */
class Snapshot(val json: String, val version: Long)

/**
 * Where [LocalApiService] keeps its data. Every call is blocking, so callers run it off the main thread.
 * [save] only succeeds when nobody saved since [Snapshot.version], so two phones never overwrite each other.
 */
interface StateStore {
    fun load(): Snapshot?

    fun save(json: String, version: Long): Boolean

    fun putFile(key: String, bytes: ByteArray)

    fun getFile(key: String): ByteArray?

    fun deleteFile(key: String)
}

/** Keeps everything on this device only. */
class DeviceStore(dir: File) : StateStore {
    private val state = File(dir, "gradtrack-local.json")
    private val uploads = File(dir, "uploads")

    override fun load(): Snapshot? = state.takeIf { it.exists() }?.let { Snapshot(it.readText(), 0) }

    override fun save(json: String, version: Long): Boolean {
        val tmp = File(state.parentFile, "${state.name}.tmp")
        tmp.writeText(json)
        return tmp.renameTo(state)
    }

    override fun putFile(key: String, bytes: ByteArray) {
        uploads.mkdirs()
        File(uploads, key).writeBytes(bytes)
    }

    override fun getFile(key: String): ByteArray? = File(uploads, key).takeIf { it.exists() }?.readBytes()

    override fun deleteFile(key: String) {
        File(uploads, key).delete()
    }
}

/**
 * Shares the state between phones through Supabase: one row in `app_state` for the data and the
 * `gradtrack` storage bucket for uploaded files. A stand-in for the real backend, good enough for demos.
 */
class SupabaseStore(baseUrl: String, private val key: String, private val client: OkHttpClient) : StateStore {
    private val base = baseUrl.trimEnd('/')
    private val gson = Gson()
    private val json = "application/json".toMediaType()

    override fun load(): Snapshot? {
        val body = call(Request.Builder().url("$base/rest/v1/app_state?id=eq.$ROW&select=data,version").get())
        val rows = JsonParser.parseString(body).asJsonArray
        if (rows.isEmpty) return null
        val row = rows[0].asJsonObject
        return Snapshot(row["data"].asString, row["version"].asLong)
    }

    override fun save(json: String, version: Long): Boolean {
        val next = gson.toJson(mapOf("id" to ROW, "data" to json, "version" to version + 1))
        if (version == 0L) {
            // First save ever: inserting fails with 409 if another phone got there first.
            val request = Request.Builder().url("$base/rest/v1/app_state").post(next.toRequestBody(this.json))
            return client.newCall(request.auth().build()).execute().use { response ->
                when {
                    response.isSuccessful -> true
                    response.code == 409 -> false
                    else -> throw IOException("Supabase insert failed (${response.code}): ${response.body.string()}")
                }
            }
        }
        val request =
            Request.Builder()
                .url("$base/rest/v1/app_state?id=eq.$ROW&version=eq.$version")
                .patch(next.toRequestBody(this.json))
                .header("Prefer", "return=representation")
        return JsonParser.parseString(call(request)).asJsonArray.size() == 1
    }

    override fun putFile(key: String, bytes: ByteArray) {
        val request =
            Request.Builder()
                .url("$base/storage/v1/object/$BUCKET/$key")
                .post(bytes.toRequestBody("application/octet-stream".toMediaType()))
                .header("x-upsert", "true")
        call(request)
    }

    override fun getFile(key: String): ByteArray? =
        client.newCall(Request.Builder().url("$base/storage/v1/object/$BUCKET/$key").get().auth().build()).execute().use { response ->
            if (response.isSuccessful) response.body.bytes() else null
        }

    override fun deleteFile(key: String) {
        client.newCall(Request.Builder().url("$base/storage/v1/object/$BUCKET/$key").delete().auth().build()).execute().close()
    }

    private fun call(request: Request.Builder): String =
        client.newCall(request.auth().build()).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) throw IOException("Supabase request failed (${response.code}): $body")
            body
        }

    /** New publishable keys go in `apikey` only; legacy anon keys are JWTs and also go in Authorization. */
    private fun Request.Builder.auth(): Request.Builder =
        header("apikey", key).apply { if (key.startsWith("eyJ")) header("Authorization", "Bearer $key") }

    private companion object {
        const val ROW = 1
        const val BUCKET = "gradtrack"
    }
}
