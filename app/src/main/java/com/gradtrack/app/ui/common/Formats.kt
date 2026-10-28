package com.gradtrack.app.ui.common

import android.content.Context
import android.text.format.DateUtils
import com.gradtrack.app.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private fun formatter(pattern: String) = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())

fun String?.toLocalDateOrNull(): LocalDate? {
    if (isNullOrBlank()) return null
    return runCatching {
        if (length <= 10) LocalDate.parse(this) else Instant.parse(this).atZone(ZoneId.systemDefault()).toLocalDate()
    }.getOrNull()
}

fun String?.asDate(): String = toLocalDateOrNull()?.format(formatter("d MMM yyyy")) ?: "—"

fun String?.asShortDate(): String = toLocalDateOrNull()?.format(formatter("d MMM")) ?: "—"

fun String.asRelativeTime(context: Context): String {
    val instant = runCatching { Instant.parse(this) }.getOrNull() ?: return asDate()
    if (System.currentTimeMillis() - instant.toEpochMilli() < DateUtils.MINUTE_IN_MILLIS) return context.getString(R.string.just_now)
    return DateUtils.getRelativeTimeSpanString(instant.toEpochMilli(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
}

fun String?.dueLabel(context: Context): String {
    val date = toLocalDateOrNull() ?: return ""
    val days = ChronoUnit.DAYS.between(LocalDate.now(), date)
    return context.getString(
        when {
            days < 0 -> R.string.due_overdue
            days <= 3 -> R.string.due_soon
            else -> R.string.due_upcoming
        },
    )
}

fun com.gradtrack.app.data.model.User.greetingName(): String? =
    if (universityId != null && fullName.endsWith(" $universityId")) null else fullName.removePrefix("Dr. ").substringBefore(' ')

fun String.initials(): String =
    split(' ', '.').filter { it.isNotBlank() && it[0].isLetter() && it.lowercase() != "dr" }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { first().uppercase() }
