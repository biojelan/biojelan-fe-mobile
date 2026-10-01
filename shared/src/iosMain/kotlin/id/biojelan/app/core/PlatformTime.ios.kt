package id.biojelan.app.core

import platform.Foundation.*

internal actual fun nowEpochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

internal actual fun localUtcOffsetSeconds(): Int =
    NSTimeZone.localTimeZone.secondsFromGMTForDate(NSDate()).toInt()
