package com.mintanable.notethepad.core.worker

import android.os.Build
import androidx.annotation.RequiresApi
import com.mintanable.notethepad.feature_settings.domain.model.BackupFrequency
import java.time.Duration
import java.time.ZonedDateTime

object ScheduleHelper{

    @RequiresApi(Build.VERSION_CODES.O)
    fun calculateInitialDelay(
        hour: Int,
        minute: Int,
        frequency: BackupFrequency
    ): Long {

        val now = ZonedDateTime.now()
        var nextRun = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)

        if (nextRun.isBefore(now)) {
            nextRun = when (frequency) {
                BackupFrequency.DAILY -> nextRun.plusDays(1)
                BackupFrequency.WEEKLY -> nextRun.plusWeeks(1)
                BackupFrequency.MONTHLY -> nextRun.plusMonths(1)
                else -> nextRun.plusWeeks(1)
            }
        }

        return Duration.between(now, nextRun).toMillis()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun calculateNextMonthlyDelay(
        hour: Int,
        minute: Int
    ): Long {

        val now = ZonedDateTime.now()
            .withSecond(0)
            .withNano(0)

        val dayOfMonth = now.dayOfMonth

        var nextRun = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)

        if (!nextRun.isAfter(now)) {

            val nextMonth = now.plusMonths(1)
            val maxDayNextMonth = nextMonth.toLocalDate().lengthOfMonth()


            nextRun = nextMonth
                .withDayOfMonth(minOf(dayOfMonth, maxDayNextMonth))
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0)
        }

        val delay = Duration.between(now, nextRun).toMillis()
        return maxOf(delay, 0L)
    }
}