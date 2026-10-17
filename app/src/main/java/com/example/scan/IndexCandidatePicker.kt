package com.example.scan

/** A number seen on the meter in one camera frame, with the height of its digits relative to the aimed area. */
data class Detection(val digits: String, val heightRatio: Float)

/** A number the user may want to use as the new index. */
data class IndexSuggestion(
    val value: Long,
    val display: String,
    val plausible: Boolean = true,
    val note: String? = null
)

/**
 * Chooses the most likely meter index from what the camera read in the latest frames.
 *
 * With a previous index the answer must be able to follow it: not lower, and not an unrealistic
 * jump. Everything else (years, serial numbers, plate texts) is dropped instead of being shown.
 */
object IndexCandidatePicker {

    const val MIN_DIGITS = 3

    /**
     * @param frames what each of the latest frames contained
     * @param minVotes in how many frames a number must appear before it is considered stable
     * @param previousIndex the last saved index of this meter, if any
     * @param maxJump the biggest plausible increase since the previous index
     */
    fun suggest(
        frames: List<List<Detection>>,
        minVotes: Int,
        previousIndex: Long?,
        maxJump: Long,
        limit: Int = 3
    ): List<IndexSuggestion> {
        val votes = HashMap<Long, Int>()
        val heightSum = HashMap<Long, Float>()
        val heightCount = HashMap<Long, Int>()
        for (frame in frames) {
            val seenInFrame = HashSet<Long>()
            for (detection in frame) {
                // Leading zeros are drums that are still at 0 ("02901" is 2901)
                val value = detection.digits.toLongOrNull() ?: continue
                if (value <= 0L) continue
                if (seenInFrame.add(value)) votes[value] = (votes[value] ?: 0) + 1
                heightSum[value] = (heightSum[value] ?: 0f) + detection.heightRatio
                heightCount[value] = (heightCount[value] ?: 0) + 1
            }
        }

        val safeJump = maxJump.coerceAtLeast(1L)
        val previousLength = previousIndex?.toString()?.length

        val scored = ArrayList<Pair<Long, Double>>()
        for ((value, count) in votes) {
            if (count < minVotes) continue
            if (previousIndex != null && (value < previousIndex || value - previousIndex > safeJump)) continue

            val height = (heightSum[value] ?: 0f) / (heightCount[value] ?: 1)
            var score = count.toDouble() + height * 10.0
            if (previousIndex != null) {
                // Closer to the previous index is likelier
                score += 5.0 - (value - previousIndex).toDouble() / safeJump * 5.0
                if (value.toString().length == previousLength) score += 2.0
            }
            scored.add(value to score)
        }

        return scored.sortedByDescending { it.second }
            .take(limit)
            .map { IndexSuggestion(value = it.first, display = it.first.toString()) }
    }
}
