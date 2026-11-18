package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.utils.DateUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HabitWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = runCatching { goAsync() }.getOrNull()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                updateWidgetsNow(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_HABIT -> {
                val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
                if (habitId > 0L) {
                    val pendingResult = runCatching { goAsync() }.getOrNull()
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        try {
                            toggleHabitAndRefresh(context, habitId)
                        } finally {
                            pendingResult?.finish()
                        }
                    }
                }
            }
            ACTION_REFRESH_WIDGET,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val pendingResult = runCatching { goAsync() }.getOrNull()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        refreshAllWidgetsNow(context)
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            else -> super.onReceive(context, intent)
        }
    }

    companion object {
        const val ACTION_TOGGLE_HABIT = "com.example.widget.ACTION_TOGGLE_HABIT"
        const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_WIDGET"
        const val EXTRA_HABIT_ID = "extra_habit_id"

        internal suspend fun toggleHabitAndRefresh(context: Context, habitId: Long) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                HabitWidgetEntryPoint::class.java
            )
            val habitRepository = entryPoint.habitRepository()
            val habit = habitRepository.getHabitById(habitId)
            if (habit != null) {
                habitRepository.incrementHabitProgress(habit, DateUtils.today())
            }
            refreshAllWidgetsNow(context)
        }

        internal suspend fun refreshAllWidgetsNow(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, HabitWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                updateWidgetsNow(context, appWidgetManager, ids)
            }
        }

        internal suspend fun updateWidgetsNow(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                HabitWidgetEntryPoint::class.java
            )
            val snapshot = HabitWidgetDataLoader.loadSnapshot(
                habitRepository = entryPoint.habitRepository(),
                gamificationRepository = entryPoint.gamificationRepository()
            )
            for (appWidgetId in appWidgetIds) {
                val views = buildRemoteViews(context, appWidgetId, snapshot)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
            @Suppress("DEPRECATION")
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_habit_list)
        }

        internal fun buildRemoteViews(
            context: Context,
            appWidgetId: Int,
            snapshot: HabitWidgetSnapshot
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_habit_tracker)

            views.setTextViewText(R.id.widget_date_text, snapshot.dateFormatted)
            views.setTextViewText(R.id.widget_level_badge, "Lv. ${snapshot.userLevel}")
            views.setTextViewText(
                R.id.widget_progress_text,
                "${snapshot.completedToday} of ${snapshot.totalToday} completed · ${snapshot.progressPercent}%"
            )
            views.setTextViewText(R.id.widget_streak_text, "Best ${snapshot.bestCurrentStreak}d")
            views.setProgressBar(R.id.widget_progress_bar, 100, snapshot.progressPercent, false)

            // Header & empty view open the app
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_open_app_area, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty_view, openAppPendingIntent)

            // Refresh button broadcast
            val refreshIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                1000 + appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

            // Scrollable ListView adapter
            val serviceIntent = Intent(context, HabitWidgetRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            @Suppress("DEPRECATION")
            views.setRemoteAdapter(R.id.widget_habit_list, serviceIntent)
            views.setEmptyView(R.id.widget_habit_list, R.id.widget_empty_view)
            views.setViewVisibility(
                R.id.widget_empty_view,
                if (snapshot.items.isEmpty()) View.VISIBLE else View.GONE
            )

            // Template PendingIntent for tapping any habit row to complete/increment
            val toggleTemplateIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_HABIT
            }
            val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            val toggleTemplatePendingIntent = PendingIntent.getBroadcast(
                context,
                2000 + appWidgetId,
                toggleTemplateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag
            )
            views.setPendingIntentTemplate(R.id.widget_habit_list, toggleTemplatePendingIntent)

            return views
        }
    }
}

class AndroidWidgetUpdateNotifier(
    private val context: Context
) : WidgetUpdateNotifier {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun notifyHabitsUpdated() {
        scope.launch {
            runCatching { HabitWidgetProvider.refreshAllWidgetsNow(context) }
        }
    }
}
