package com.ht.intelza.ui.common

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

fun formatDate(millis: Long): String =
    dateFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

fun formatDateTime(millis: Long): String =
    dateTimeFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

/** Formats a 0–1 fraction as a whole percentage, e.g. 0.756 -> "76%". */
fun formatPercent(fraction: Float): String =
    NumberFormat.getPercentInstance().format(fraction.toDouble())

fun percentValue(fraction: Float): Int = (fraction * 100).roundToInt()
