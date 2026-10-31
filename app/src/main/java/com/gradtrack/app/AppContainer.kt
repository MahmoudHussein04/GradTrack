package com.gradtrack.app

import android.content.Context
import java.io.File
import com.gradtrack.app.data.SessionStore
import com.gradtrack.app.data.local.DeviceStore
import com.gradtrack.app.data.local.FirebaseStore
import com.gradtrack.app.data.local.LocalApiService
import com.gradtrack.app.data.local.StateStore
import com.gradtrack.app.data.local.SupabaseStore
import com.gradtrack.app.data.remote.ApiService
import com.gradtrack.app.data.remote.NetworkModule
import com.gradtrack.app.data.repository.AuthRepository
import com.gradtrack.app.data.repository.DashboardRepository
import com.gradtrack.app.data.repository.FileRepository
import com.gradtrack.app.data.repository.NotificationRepository
import com.gradtrack.app.data.repository.ProjectRepository
import com.gradtrack.app.data.repository.ReviewRepository
import com.gradtrack.app.data.repository.TaskRepository
import com.gradtrack.app.data.repository.TeamRepository
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

class AppContainer(context: Context) {
    val session = SessionStore(context)

    /** With Firebase (google-services.json) or Supabase (local.properties) set up, every phone shares one copy of the data; otherwise it stays on this device. */
    private val store: StateStore =
        when {
            BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() ->
                FirebaseStore(context, BuildConfig.FIREBASE_PROJECT_ID, BuildConfig.FIREBASE_APP_ID, BuildConfig.FIREBASE_API_KEY)
            BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank() ->
                SupabaseStore(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY, OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build())
            else -> DeviceStore(context.filesDir)
        }

    private val api: ApiService =
        if (BuildConfig.USE_LOCAL_API) LocalApiService(context, session, openLogin = BuildConfig.LOCAL_OPEN_LOGIN, store = store) else NetworkModule.createApi(session)

    val auth = AuthRepository(api, session)
    val dashboard = DashboardRepository(api)
    val projects = ProjectRepository(api)
    val team = TeamRepository(api)
    val tasks = TaskRepository(api)
    val notifications = NotificationRepository(api)
    val reviews = ReviewRepository(api)
    val files = FileRepository(api, File(context.cacheDir, "shared"))
}
