package com.example.widget

interface WidgetUpdateNotifier {
    fun notifyHabitsUpdated()

    companion object {
        val NO_OP = object : WidgetUpdateNotifier {
            override fun notifyHabitsUpdated() = Unit
        }
    }
}
