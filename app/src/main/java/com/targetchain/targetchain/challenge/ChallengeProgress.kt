package com.targetchain.targetchain.challenge

/** Progress through an alarm's challenges, which are completed one by one in order. */
data class ChallengeProgress(val total: Int, val completed: Int) {

    /** Index of the challenge to show, or null when there is none left. */
    val currentIndex: Int?
        get() = completed.takeIf { it < total }

    /** The alarm stops only once its last challenge is completed. */
    val isFinished: Boolean
        get() = total > 0 && completed >= total
}

/** Copy of this list with the item at [from] moved to [to]; unchanged if either is out of range. */
fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}
