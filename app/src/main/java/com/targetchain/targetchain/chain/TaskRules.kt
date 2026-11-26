package com.targetchain.targetchain.chain

/** A task can be saved once it has at least one challenge and a valid duration. */
fun isTaskComplete(challengeCount: Int, durationSeconds: Int?): Boolean =
    challengeCount > 0 && isValidTaskDuration(durationSeconds)
