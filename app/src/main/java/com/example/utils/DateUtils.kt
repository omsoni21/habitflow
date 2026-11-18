package com.example.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {
    private val isoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val displayTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d")

    fun today(): LocalDate = LocalDate.now()

    fun formatIsoDate(date: LocalDate): String = date.format(isoDateFormatter)

    fun parseIsoDate(dateStr: String): LocalDate =
        runCatching { LocalDate.parse(dateStr, isoDateFormatter) }.getOrElse { LocalDate.now() }

    fun formatTime24(time: LocalTime): String = time.format(timeFormatter)

    fun parseTime24(timeStr: String?): LocalTime? =
        timeStr?.takeIf { it.isNotBlank() }?.let {
            runCatching { LocalTime.parse(it, timeFormatter) }.getOrNull()
        }

    fun formatDisplayTime(time: LocalTime): String = time.format(displayTimeFormatter)

    fun formatFullDate(date: LocalDate): String = date.format(fullDateFormatter)

    fun formatMonthYear(yearMonth: YearMonth): String = yearMonth.format(monthYearFormatter)

    fun formatShortDate(date: LocalDate): String = date.format(shortDateFormatter)

    fun shortDayName(dayOfWeek: DayOfWeek): String =
        dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    fun singleLetterDayName(dayOfWeek: DayOfWeek): String =
        dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())

    fun greetingForHour(hour: Int = LocalTime.now().hour): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }

    fun encodeDays(days: Set<DayOfWeek>): String =
        days.sortedBy { it.value }.joinToString(",") { it.value.toString() }

    fun decodeDays(raw: String): Set<DayOfWeek> {
        if (raw.isBlank()) return DayOfWeek.entries.toSet()
        return raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .mapNotNull { value -> DayOfWeek.entries.firstOrNull { it.value == value } }
            .toSet()
            .ifEmpty { DayOfWeek.entries.toSet() }
    }
}
