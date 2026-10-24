package com.gradtrack.app.data.remote

import com.gradtrack.app.BuildConfig
import com.gradtrack.app.data.SessionStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Adds the bearer token and reports 401s (except on the login call itself) so the app can sign the user out. */
private class AuthInterceptor(private val session: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = session.token
        val authed = if (token != null) request.newBuilder().header("Authorization", "Bearer $token").build() else request
        val response = chain.proceed(authed)
        if (response.code == 401 && !request.url.encodedPath.endsWith("auth/login")) session.onUnauthorized()
        return response
    }
}

object NetworkModule {
    fun createApi(session: SessionStore): ApiService {
        val client =
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(AuthInterceptor(session))
                .apply {
                    if (BuildConfig.DEBUG) {
                        addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                    }
                }
                .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
