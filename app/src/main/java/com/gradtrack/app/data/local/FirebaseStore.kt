package com.gradtrack.app.data.local

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Shares the state between phones through Cloud Firestore: one document holds the data and uploaded
 * files are split into ~900 KB chunks in a second collection. Firestore alone fits the free Spark plan,
 * which Cloud Storage no longer does. A stand-in until the real backend exists.
 */
class FirebaseStore(context: Context, projectId: String, appId: String, apiKey: String) : StateStore {
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance(
            FirebaseApp.getApps(context).firstOrNull { it.name == APP }
                ?: FirebaseApp.initializeApp(context, FirebaseOptions.Builder().setProjectId(projectId).setApplicationId(appId).setApiKey(apiKey).build(), APP),
        )
    private val state = firestore.collection(COLLECTION).document("state")
    private val files = firestore.collection(FILES)

    override fun load(): Snapshot? {
        val doc = state.get(Source.SERVER).await()
        if (!doc.exists()) return null
        return Snapshot(doc.getString("data").orEmpty(), doc.getLong("version") ?: 0)
    }

    override fun save(json: String, version: Long): Boolean =
        firestore.runTransaction { tx ->
            val current = tx.get(state)
            val saved = if (current.exists()) current.getLong("version") ?: 0 else 0
            if (saved != version) {
                false
            } else {
                tx.set(state, mapOf("data" to json, "version" to version + 1))
                true
            }
        }.await()

    override fun putFile(key: String, bytes: ByteArray) {
        deleteFile(key)
        val parts = (bytes.indices step CHUNK_BYTES).map { bytes.copyOfRange(it, minOf(it + CHUNK_BYTES, bytes.size)) }.ifEmpty { listOf(ByteArray(0)) }
        parts.forEachIndexed { index, part -> files.document("$key.$index").set(mapOf("bytes" to Blob.fromBytes(part))).await() }
        files.document(key).set(mapOf("parts" to parts.size)).await()
    }

    override fun getFile(key: String): ByteArray? {
        val meta = files.document(key).get(Source.SERVER).await()
        if (!meta.exists()) return null
        val parts = meta.getLong("parts")?.toInt() ?: return null
        val out = ByteArrayOutputStream()
        repeat(parts) { index -> out.write(files.document("$key.$index").get(Source.SERVER).await().getBlob("bytes")?.toBytes() ?: return null) }
        return out.toByteArray()
    }

    override fun deleteFile(key: String) {
        val parts = files.document(key).get(Source.SERVER).await().takeIf { it.exists() }?.getLong("parts")?.toInt() ?: return
        files.document(key).delete().await()
        repeat(parts) { files.document("$key.$it").delete().await() }
    }

    private fun <T> Task<T>.await(): T = Tasks.await(this, TIMEOUT_SECONDS, TimeUnit.SECONDS)

    private companion object {
        const val APP = "gradtrack"
        const val COLLECTION = "gradtrack"
        const val FILES = "gradtrack_files"
        const val CHUNK_BYTES = 900_000
        const val TIMEOUT_SECONDS = 30L
    }
}
