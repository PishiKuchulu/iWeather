package com.pishi.iweather.notify

import java.time.Duration
import java.time.ZonedDateTime

/** Milliseconds from now until the next [hour]:00 local time (zone-aware, so DST changes are handled). */
internal fun millisUntilNext(hour: Int, now: ZonedDateTime = ZonedDateTime.now()): Long {
    var next = now.withHour(hour.coerceIn(0, 23)).withMinute(0).withSecond(0).withNano(0)
    if (!next.isAfter(now.plusMinutes(1))) next = next.plusDays(1)
    return Duration.between(now, next).toMillis()
}

/**
 * Stable notification ID for a dedupe key (AUD-006): a 64-bit polynomial hash folded to a
 * positive Int — deliberately wider than String.hashCode()'s 32 bits, so distinct
 * (day, city, alert-type) triples do not merge into one notification slot.
 */
internal fun alertNotificationId(key: String): Int {
    var h = 1123L
    for (ch in key) h = h * 31 + ch.code
    return ((h xor (h ushr 32)) and 0x7fff_ffffL).toInt()
}
