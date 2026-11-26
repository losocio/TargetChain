package com.targetchain.targetchain.chain

/**
 * When a task's countdown ends, kept so that changing the phone's time or time zone doesn't make it
 * longer or shorter.
 *
 * The end is scheduled on the wall clock ([endAtWallMs], epoch milliseconds: the time zone doesn't
 * affect it), because setAlarmClock only takes wall clock times. Next to it we keep an anchor on
 * the monotonic clock (elapsedRealtime), which a manual time change doesn't move: [remainingAtAnchorMs]
 * was left when elapsedRealtime was [anchorElapsedMs]. elapsedRealtime restarts at 0 on reboot, so
 * the anchor is only valid during the boot it was taken in ([bootCount]); after a reboot the wall
 * clock is trusted and a new anchor is taken.
 */
data class CountdownTiming(
    val endAtWallMs: Long,
    val anchorElapsedMs: Long,
    val remainingAtAnchorMs: Long,
    val bootCount: Int,
)

/** Readings of the phone's clocks at one moment. */
data class ClockReading(val wallMs: Long, val elapsedMs: Long, val bootCount: Int)

fun startCountdown(now: ClockReading, durationMs: Long): CountdownTiming =
    CountdownTiming(
        endAtWallMs = now.wallMs + durationMs,
        anchorElapsedMs = now.elapsedMs,
        remainingAtAnchorMs = durationMs,
        bootCount = now.bootCount,
    )

/** Time left at [now], never negative. */
fun CountdownTiming.remainingMs(now: ClockReading): Long {
    val remaining = if (now.bootCount == bootCount) {
        remainingAtAnchorMs - (now.elapsedMs - anchorElapsedMs)
    } else {
        // Rebooted: the monotonic anchor is lost, only the wall clock is left.
        endAtWallMs - now.wallMs
    }
    return remaining.coerceAtLeast(0)
}

/**
 * The same countdown re-anchored at [now]: its wall clock end moves by as much as the clock was
 * changed, so the time left stays the same. Used after a time change and after a reboot.
 */
fun CountdownTiming.reanchored(now: ClockReading): CountdownTiming {
    val remaining = remainingMs(now)
    return CountdownTiming(
        endAtWallMs = now.wallMs + remaining,
        anchorElapsedMs = now.elapsedMs,
        remainingAtAnchorMs = remaining,
        bootCount = now.bootCount,
    )
}
