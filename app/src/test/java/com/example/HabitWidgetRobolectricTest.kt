package com.example

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.HabitFlowDatabase
import com.example.data.repository.GamificationRepository
import com.example.data.repository.HabitRepository
import com.example.domain.model.Habit
import com.example.domain.model.HabitCategory
import com.example.domain.model.HabitFrequency
import com.example.utils.DateUtils
import com.example.widget.HabitWidgetDataLoader
import com.example.widget.HabitWidgetEntryPoint
import com.example.widget.HabitWidgetProvider
import com.example.widget.HabitWidgetRemoteViewsFactory
import com.example.widget.HabitWidgetSnapshot
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HabitWidgetRobolectricTest {

    @Test
    fun `manifest registers HabitWidgetProvider for AppWidgetManager`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
            setPackage(context.packageName)
        }
        val receivers = context.packageManager.queryBroadcastReceivers(intent, 0)
        val hasWidgetProvider = receivers.any {
            it.activityInfo.name == HabitWidgetProvider::class.java.name
        }
        assertTrue("Expected HabitWidgetProvider to be registered in AndroidManifest", hasWidgetProvider)
    }

    @Test
    fun `widget data loader computes today's scheduled habits and progress`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, HabitFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val gamificationRepo = GamificationRepository(
                userStatsDao = db.userStatsDao(),
                habitDao = db.habitDao(),
                completionDao = db.habitCompletionDao(),
                achievementDao = db.achievementDao()
            )
            val habitRepo = HabitRepository(
                database = db,
                habitDao = db.habitDao(),
                completionDao = db.habitCompletionDao(),
                achievementDao = db.achievementDao(),
                gamificationRepository = gamificationRepo
            )
            val today = DateUtils.today()
            val waterId = habitRepo.saveHabit(
                Habit(
                    title = "Drink Water",
                    category = HabitCategory.HEALTH,
                    frequency = HabitFrequency.DAILY,
                    target = 3,
                    targetUnit = "glasses",
                    startDate = today.minusDays(2)
                )
            )
            val readId = habitRepo.saveHabit(
                Habit(
                    title = "Read Book",
                    category = HabitCategory.LEARNING,
                    frequency = HabitFrequency.DAILY,
                    target = 1,
                    targetUnit = "session",
                    startDate = today.minusDays(2)
                )
            )
            // Archived habit should not appear
            habitRepo.saveHabit(
                Habit(
                    title = "Old Archived Habit",
                    isArchived = true,
                    startDate = today.minusDays(2)
                )
            )

            val readHabit = habitRepo.getHabitById(readId)!!
            habitRepo.toggleHabitCompletion(readHabit, today)

            val snapshot = HabitWidgetDataLoader.loadSnapshot(habitRepo, gamificationRepo, today)
            assertEquals(2, snapshot.totalToday)
            assertEquals(1, snapshot.completedToday)
            assertEquals(50, snapshot.progressPercent)
            // Incomplete habit sorted first
            assertEquals(waterId, snapshot.items.first().habitId)
            assertFalse(snapshot.items.first().isCompleted)
            assertTrue(snapshot.items.last().isCompleted)
        } finally {
            db.close()
        }
    }

    @Test
    fun `buildRemoteViews renders summary header and empty view visibility`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val snapshot = HabitWidgetSnapshot(
            dateFormatted = "Wednesday, Nov 18",
            totalToday = 4,
            completedToday = 2,
            progressPercent = 50,
            userLevel = 3,
            bestCurrentStreak = 6,
            items = emptyList()
        )

        val remoteViews = HabitWidgetProvider.buildRemoteViews(context, 42, snapshot)
        val container = FrameLayout(context)
        val inflated = remoteViews.apply(context, container)

        assertEquals(
            "Wednesday, Nov 18",
            inflated.findViewById<TextView>(R.id.widget_date_text).text.toString()
        )
        assertEquals(
            "Lv. 3",
            inflated.findViewById<TextView>(R.id.widget_level_badge).text.toString()
        )
        assertEquals(
            "2 of 4 completed · 50%",
            inflated.findViewById<TextView>(R.id.widget_progress_text).text.toString()
        )
        assertEquals(
            "Best 6d",
            inflated.findViewById<TextView>(R.id.widget_streak_text).text.toString()
        )
        assertEquals(View.VISIBLE, inflated.findViewById<View>(R.id.widget_empty_view).visibility)
    }

    @Test
    fun `toggleHabitAndRefresh increments habit progress and updates bound AppWidget`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val entryPoint = EntryPointAccessors.fromApplication(context, HabitWidgetEntryPoint::class.java)
        val habitRepo = entryPoint.habitRepository()
        val today = DateUtils.today()

        // Clean slate inside Hilt singleton DB
        habitRepo.getAllHabitEntitiesOnce().forEach { habitRepo.deleteHabit(it.id) }

        val habitId = habitRepo.saveHabit(
            Habit(
                title = "Hydrate",
                category = HabitCategory.HEALTH,
                frequency = HabitFrequency.DAILY,
                target = 2,
                targetUnit = "glasses",
                startDate = today
            )
        )

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val shadowWidgetManager = Shadows.shadowOf(appWidgetManager)
        val widgetId = 77
        val providerInfo = android.appwidget.AppWidgetProviderInfo().apply {
            provider = ComponentName(context, HabitWidgetProvider::class.java)
            initialLayout = R.layout.widget_habit_tracker
        }
        shadowWidgetManager.addBoundWidget(widgetId, providerInfo)
        appWidgetManager.updateAppWidget(
            widgetId,
            HabitWidgetProvider.buildRemoteViews(
                context,
                widgetId,
                HabitWidgetDataLoader.loadSnapshot(habitRepo, entryPoint.gamificationRepository(), today)
            )
        )

        // Verify RemoteViewsFactory renders the initial 0/2 row
        val factory = HabitWidgetRemoteViewsFactory(context)
        factory.onDataSetChanged()
        assertEquals(1, factory.count)
        val initialRowViews = factory.getViewAt(0)!!
        val initialRow = initialRowViews.apply(context, FrameLayout(context))
        assertEquals("Hydrate", initialRow.findViewById<TextView>(R.id.widget_item_title).text.toString())
        assertTrue(
            initialRow.findViewById<TextView>(R.id.widget_item_subtitle).text.toString().contains("0/2 glasses")
        )

        // Tap once -> 1/2
        HabitWidgetProvider.toggleHabitAndRefresh(context, habitId)
        assertFalse(habitRepo.isCompletedOnDate(habitId, today))

        factory.onDataSetChanged()
        val afterFirstTap = factory.getViewAt(0)!!.apply(context, FrameLayout(context))
        assertTrue(
            afterFirstTap.findViewById<TextView>(R.id.widget_item_subtitle).text.toString().contains("1/2 glasses")
        )

        // Tap twice -> 2/2 (completed!)
        HabitWidgetProvider.toggleHabitAndRefresh(context, habitId)
        assertTrue(habitRepo.isCompletedOnDate(habitId, today))

        val updatedSnapshot = HabitWidgetDataLoader.loadSnapshot(
            habitRepo,
            entryPoint.gamificationRepository(),
            today
        )
        assertEquals(1, updatedSnapshot.completedToday)
        assertEquals(100, updatedSnapshot.progressPercent)

        val boundViews = shadowWidgetManager.getViewFor(widgetId)
        assertNotNull(boundViews)
        assertEquals(
            "1 of 1 completed · 100%",
            boundViews.findViewById<TextView>(R.id.widget_progress_text).text.toString()
        )

        // Clean up
        habitRepo.deleteHabit(habitId)
    }
}
