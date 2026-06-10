package com.myhebnu.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * 4 Glance widget receivers, one per widget size.
 * Each appears as a separate entry in the launcher's widget picker.
 */
class MicroWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ScheduleMicroWidget()
}

class MediumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ScheduleMediumWidget()
}

class LargeListReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ScheduleLargeListWidget()
}
