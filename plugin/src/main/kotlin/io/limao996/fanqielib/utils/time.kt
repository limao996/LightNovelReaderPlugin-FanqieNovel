package io.limao996.fanqielib.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SuppressLint("SimpleDateFormat")
fun legacyToLocalDateTime(secondsTimestamp: Long): LocalDateTime {
    val date = Date(secondsTimestamp * 1000)
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val formattedString = sdf.format(date)

    return LocalDateTime.parse(formattedString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
}