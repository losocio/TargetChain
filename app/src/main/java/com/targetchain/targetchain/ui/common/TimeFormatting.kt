package com.targetchain.targetchain.ui.common

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Time of day ([minutesOfDay] since midnight) in the user's locale, e.g. "07:30" or "7:30 AM". */
fun formatTime(minutesOfDay: Int): String =
    LocalTime.of(minutesOfDay / 60, minutesOfDay % 60)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
