package com.example.utils

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.domain.model.Habit
import com.example.domain.model.HabitStreakInfo
import com.example.domain.usecase.LevelStatus
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object ShareMilestoneHelper {

    fun buildHabitStreakShareText(
        habit: Habit,
        streakInfo: HabitStreakInfo,
        levelStatus: LevelStatus? = null
    ): String {
        val ratePercent = (streakInfo.completionRate * 100f).roundToInt()
        val levelSuffix = levelStatus?.let {
            "\nLevel ${it.level} (${it.title}) • ${it.totalXp} XP on HabitFlow"
        } ?: ""

        return buildString {
            append("🔥 ${streakInfo.currentStreak}-day streak on \"${habit.title}\"!")
            append("\nBest Streak: ${streakInfo.longestStreak} days • $ratePercent% consistency (${streakInfo.totalCompletedDays} days completed)")
            append(levelSuffix)
            append("\n#HabitFlow #HabitTracker #Consistency")
        }
    }

    fun buildLevelAndAchievementsShareText(
        userName: String,
        levelStatus: LevelStatus,
        bestStreak: Int,
        unlockedAchievementsCount: Int,
        totalAchievementsCount: Int
    ): String {
        return buildString {
            append("🏆 $userName reached Level ${levelStatus.level} (${levelStatus.title}) on HabitFlow!")
            append("\n⚡ ${levelStatus.totalXp} Total XP • ${levelStatus.xpRemainingForNextLevel} XP to Level ${levelStatus.level + 1}")
            append("\n🔥 Best Streak: ${bestStreak} days • 🎖️ $unlockedAchievementsCount/$totalAchievementsCount Achievements Unlocked")
            append("\n#HabitFlow #PersonalGrowth #HabitStreak")
        }
    }

    fun buildShareIntent(
        context: Context,
        shareText: String,
        chooserTitle: String,
        imageUri: Uri? = null,
        subject: String = "My HabitFlow Progress"
    ): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, shareText)
            if (imageUri != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                clipData = ClipData.newRawUri(subject, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }
        return Intent.createChooser(sendIntent, chooserTitle).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun shareHabitStreak(
        context: Context,
        habit: Habit,
        streakInfo: HabitStreakInfo,
        levelStatus: LevelStatus? = null
    ) {
        val shareText = buildHabitStreakShareText(habit, streakInfo, levelStatus)
        val imageUri = createMilestoneCardUri(
            context = context,
            headline = "${streakInfo.currentStreak}-Day Streak",
            subheadline = habit.title,
            metricLine = "Best: ${streakInfo.longestStreak}d  •  ${(streakInfo.completionRate * 100f).roundToInt()}% Rate",
            footerLine = levelStatus?.let { "Level ${it.level} ${it.title} • ${it.totalXp} XP" } ?: "HabitFlow Smart Tracker"
        )
        val chooser = buildShareIntent(
            context = context,
            shareText = shareText,
            chooserTitle = "Share Habit Streak",
            imageUri = imageUri,
            subject = "${habit.title} — ${streakInfo.currentStreak}-Day Streak"
        )
        context.startActivity(chooser)
    }

    fun shareLevelAndAchievements(
        context: Context,
        userName: String,
        levelStatus: LevelStatus,
        bestStreak: Int,
        unlockedAchievementsCount: Int,
        totalAchievementsCount: Int
    ) {
        val shareText = buildLevelAndAchievementsShareText(
            userName = userName,
            levelStatus = levelStatus,
            bestStreak = bestStreak,
            unlockedAchievementsCount = unlockedAchievementsCount,
            totalAchievementsCount = totalAchievementsCount
        )
        val imageUri = createMilestoneCardUri(
            context = context,
            headline = "Level ${levelStatus.level} • ${levelStatus.title}",
            subheadline = "$userName's HabitFlow Journey",
            metricLine = "${levelStatus.totalXp} XP  •  Best Streak: ${bestStreak}d",
            footerLine = "$unlockedAchievementsCount of $totalAchievementsCount Achievements Unlocked"
        )
        val chooser = buildShareIntent(
            context = context,
            shareText = shareText,
            chooserTitle = "Share Level & Achievements",
            imageUri = imageUri,
            subject = "HabitFlow Level ${levelStatus.level} Achievement"
        )
        context.startActivity(chooser)
    }

    private fun createMilestoneCardUri(
        context: Context,
        headline: String,
        subheadline: String,
        metricLine: String,
        footerLine: String
    ): Uri? = runCatching {
        val width = 1080
        val height = 560
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                intArrayOf(
                    Color.parseColor("#4F46E5"),
                    Color.parseColor("#7C3AED"),
                    Color.parseColor("#0284C7")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 48f, 48f, bgPaint)

        val cardOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(38, 255, 255, 255)
        }
        canvas.drawRoundRect(RectF(44f, 44f, width - 44f, height - 44f), 36f, 36f, cardOverlayPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 224, 231, 255)
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("HABITFLOW MILESTONE", 88f, 118f, brandPaint)

        val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 72f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(headline.take(28), 88f, 232f, headlinePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(240, 241, 245, 249)
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(subheadline.take(36), 88f, 306f, subPaint)

        val metricPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FDE68A")
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(metricLine.take(44), 88f, 392f, metricPaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(215, 255, 255, 255)
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(footerLine.take(48), 88f, 466f, footerPaint)

        val shareDir = File(context.cacheDir, "shares").apply { mkdirs() }
        val file = File(shareDir, "habitflow_milestone.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        bitmap.recycle()

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}
