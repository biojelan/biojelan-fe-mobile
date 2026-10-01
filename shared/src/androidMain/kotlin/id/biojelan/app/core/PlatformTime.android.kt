package id.biojelan.app.core

import java.util.TimeZone

internal actual fun nowEpochMillis(): Long = System.currentTimeMillis()

internal actual fun localUtcOffsetSeconds(): Int =
    TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000
