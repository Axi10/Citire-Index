package com.example.scan

/** A number seen on the meter in one camera frame, with the height of its digits relative to the frame. */
data class Detection(val digits: String, val heightRatio: Float)

/** A number the user may want to use as the new index. */
data class IndexSuggestion(
    val value: Long,
    val display: String,
    /** False when the number does not fit after the previous index (lower, or an implausibly big jump). */
    val plausible: Boolean,
    val note: String? = null
)

/**
 * Chooses the most likely meter index from what the camera read in the latest frames.
 *
 * Reading a meter is harder than it looks: besides the index the picture has serial numbers,
 * years and plate texts, and drum meters show red decimals right next to the index. Two things
 * the app knows help: the index can never be lower than the previous one, and it cannot jump by
 * an unrealistic amount between two readings.
 */
object IndexCandidatePicker {

    const val MIN_DIGITS = 3

    // Drum gas meters show up to three red decimal digits after the index
    const val MAX_DECIMAL_DIGITS = 3

    private data class Variant(val value: Long, val trimmed: Int, val votes: Int, val height: Float)

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
        limit: Int = 4
    ): List<IndexSuggestion> {
        // votes = number of frames a number appears in; height = average height of its digits
        val votes = HashMap<String, Int>()
        val heightSum = HashMap<String, Float>()
        val heightCount = HashMap<String, Int>()
        for (frame in frames) {
            val seenInFrame = HashSet<String>()
            for (detection in frame) {
                if (seenInFrame.add(detection.digits)) {
                    votes[detection.digits] = (votes[detection.digits] ?: 0) + 1
                }
                heightSum[detection.digits] = (heightSum[detection.digits] ?: 0f) + detection.heightRatio
                heightCount[detection.digits] = (heightCount[detection.digits] ?: 0) + 1
            }
        }

        val variants = ArrayList<Variant>()
        for ((digits, count) in votes) {
            if (count < minVotes) continue
            val height = (heightSum[digits] ?: 0f) / (heightCount[digits] ?: 1)
            val raw = digits.toLongOrNull() ?: continue
            variants.add(Variant(raw, 0, count, height))

            // With a previous index to compare against, also try the number without its last
            // digits: they may be the red decimals of the meter
            if (previousIndex != null) {
                for (k in 1..MAX_DECIMAL_DIGITS) {
                    if (digits.length - k < MIN_DIGITS) break
                    val trimmed = digits.dropLast(k).toLongOrNull() ?: continue
                    variants.add(Variant(trimmed, k, count, height))
                }
            }
        }
        if (variants.isEmpty()) return emptyList()

        if (previousIndex == null) {
            // Nothing to compare with: trust frame count and digit size
            return distinctBest(variants.sortedByDescending { it.votes + it.height * 10.0 })
                .take(limit)
                .map { IndexSuggestion(it.value, it.value.toString(), plausible = true) }
        }

        val previousLength = previousIndex.toString().length
        val safeJump = maxJump.coerceAtLeast(1L)

        fun isPlausible(value: Long): Boolean = value >= previousIndex && value - previousIndex <= safeJump

        fun score(variant: Variant): Double {
            var score = variant.votes.toDouble() + variant.height * 10.0
            if (variant.value.toString().length == previousLength) score += 2.0
            score -= variant.trimmed * 0.5
            if (isPlausible(variant.value)) {
                // Plausible numbers always beat implausible ones; closer to the previous index is likelier
                score += 100.0 - (variant.value - previousIndex).toDouble() / safeJump * 5.0
            }
            return score
        }

        return distinctBest(variants.sortedByDescending(::score))
            .take(limit)
            .map { variant ->
                IndexSuggestion(
                    value = variant.value,
                    display = variant.value.toString(),
                    plausible = isPlausible(variant.value),
                    note = if (variant.trimmed > 0) {
                        if (variant.trimmed == 1) "Fara ultima cifra (zecimala rosie)" else "Fara ultimele ${variant.trimmed} cifre (zecimalele rosii)"
                    } else {
                        null
                    }
                )
            }
    }

    /** Keeps the first (best ranked) variant of each distinct value. */
    private fun distinctBest(sorted: List<Variant>): List<Variant> {
        val seen = LinkedHashMap<Long, Variant>()
        for (variant in sorted) {
            if (!seen.containsKey(variant.value)) seen[variant.value] = variant
        }
        return seen.values.toList()
    }
}
