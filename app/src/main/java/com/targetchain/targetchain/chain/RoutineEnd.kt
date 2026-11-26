package com.targetchain.targetchain.chain

/**
 * Earliest time a routine can end: its start time plus the durations of all its tasks, not
 * counting the time spent on challenges.
 *
 * @property secondsOfDay time of day, in seconds since midnight.
 * @property daysLater how many days after the start day it ends (0 = the same day).
 */
data class EarliestEnd(val secondsOfDay: Int, val daysLater: Int)

/** Null while any task has no valid duration. */
fun earliestEnd(startMinutesOfDay: Int, taskDurationsSeconds: List<Int?>): EarliestEnd? {
    if (taskDurationsSeconds.isEmpty() || !taskDurationsSeconds.all(::isValidTaskDuration)) return null
    val endSeconds = startMinutesOfDay * 60L + taskDurationsSeconds.sumOf { it!!.toLong() }
    return EarliestEnd(
        secondsOfDay = (endSeconds % SECONDS_PER_DAY).toInt(),
        daysLater = (endSeconds / SECONDS_PER_DAY).toInt(),
    )
}

private const val SECONDS_PER_DAY = 24 * 60 * 60
