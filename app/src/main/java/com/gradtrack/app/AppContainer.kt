package com.gradtrack.app

import android.content.Context
import com.gradtrack.app.data.SessionStore
import com.gradtrack.app.data.local.LocalApiService
import com.gradtrack.app.data.remote.ApiService
import com.gradtrack.app.data.remote.NetworkModule
import com.gradtrack.app.data.repository.AuthRepository
import com.gradtrack.app.data.repository.DashboardRepository
import com.gradtrack.app.data.repository.NotificationRepository
import com.gradtrack.app.data.repository.ProjectRepository
import com.gradtrack.app.data.repository.ReviewRepository
import com.gradtrack.app.data.repository.TaskRepository
import com.gradtrack.app.data.repository.TeamRepository

/** Hand-written dependency graph: small enough that a DI framework would only add build time. */
class AppContainer(context: Context) {
    val session = SessionStore(context)

    private val api: ApiService = if (BuildConfig.USE_LOCAL_API) LocalApiService(context, session, openLogin = BuildConfig.LOCAL_OPEN_LOGIN) else NetworkModule.createApi(session)

    val auth = AuthRepository(api, session)
    val dashboard = DashboardRepository(api)
    val projects = ProjectRepository(api)
    val team = TeamRepository(api)
    val tasks = TaskRepository(api)
    val notifications = NotificationRepository(api)
    val reviews = ReviewRepository(api)
}
