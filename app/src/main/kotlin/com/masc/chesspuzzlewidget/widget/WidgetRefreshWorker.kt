package com.masc.chesspuzzlewidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Periodically re-renders every placed widget with no network I/O — needed because a widget's
 * RemoteViews only repaint on user interaction otherwise, so date-dependent UI (the daily solved
 * counter) would keep showing yesterday's value until the user happened to tap something today.
 */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val manager = AppWidgetManager.getInstance(applicationContext)
        val ids = manager.getAppWidgetIds(ComponentName(applicationContext, ChessPuzzleWidgetProvider::class.java))
        ids.forEach { appWidgetId -> WidgetUpdater.render(applicationContext, appWidgetId) }
        return Result.success()
    }
}
