package com.example.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking

class HabitWidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return HabitWidgetRemoteViewsFactory(applicationContext)
    }
}

internal class HabitWidgetRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<HabitWidgetItem> = emptyList()

    override fun onCreate() {
        items = emptyList()
    }

    override fun onDataSetChanged() {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            HabitWidgetEntryPoint::class.java
        )
        items = runBlocking {
            HabitWidgetDataLoader.loadSnapshot(
                habitRepository = entryPoint.habitRepository(),
                gamificationRepository = entryPoint.gamificationRepository()
            ).items
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        val item = items.getOrNull(position) ?: return null
        val views = RemoteViews(context.packageName, R.layout.widget_habit_item)

        views.setTextViewText(R.id.widget_item_title, item.title)

        val subtitle = if (item.isCompleted) {
            "Completed • ${item.categoryLabel}"
        } else if (item.target > 1) {
            "${item.todayValue}/${item.target} ${item.targetUnit} • ${item.categoryLabel}"
        } else {
            item.categoryLabel
        }
        views.setTextViewText(R.id.widget_item_subtitle, subtitle)
        views.setTextViewText(R.id.widget_item_streak, "${item.currentStreak}d")

        val statusIconRes = when {
            item.isCompleted -> R.drawable.ic_widget_checked
            item.target > 1 -> R.drawable.ic_widget_increment
            else -> R.drawable.ic_widget_unchecked
        }
        views.setImageViewResource(R.id.widget_item_status_icon, statusIconRes)

        val backgroundRes = if (item.isCompleted) {
            R.drawable.widget_item_completed_background
        } else {
            R.drawable.widget_item_background
        }
        views.setInt(R.id.widget_item_root, "setBackgroundResource", backgroundRes)

        val fillInIntent = Intent().apply {
            putExtra(HabitWidgetProvider.EXTRA_HABIT_ID, item.habitId)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = items.getOrNull(position)?.habitId ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
