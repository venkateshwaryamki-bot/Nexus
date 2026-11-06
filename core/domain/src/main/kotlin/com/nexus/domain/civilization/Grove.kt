package com.nexus.domain.civilization

/** Tree growth stages (blueprint "Tree Growth System"). [minutes] = completed focus minutes the tree needs. */
enum class TreeStage(val title: String, val minutes: Int) {
    SEED("Seed", 0), SPROUT("Sprout", 25), YOUNG("Young Tree", 75), MATURE("Mature Tree", 150), ANCIENT("Ancient Tree", 300);
}

data class GroveTree(val index: Int, val stage: TreeStage, val minutes: Int) {
    val next: TreeStage? get() = TreeStage.entries.getOrNull(stage.ordinal + 1)
    val minutesToNext: Int get() = next?.let { it.minutes - minutes } ?: 0
    val progressToNext: Float get() = next?.let { (minutes - stage.minutes).toFloat() / (it.minutes - stage.minutes) } ?: 1f
}

data class TreeGrowth(val index: Int, val from: TreeStage?, val to: TreeStage)

/**
 * The grove grows only from *completed* focus minutes, which makes it a visible record of real work.
 * Tree 0 is a seed from day one; a new seed is planted every [PLANT_EVERY] minutes, up to [MAX_TREES].
 * Each tree is fed by the minutes completed since it was planted. Pure and deterministic, so it is
 * derived from the reward ledger rather than stored, and can never drift out of sync.
 */
object Grove {
    const val PLANT_EVERY = 150
    const val MAX_TREES = 10

    fun compute(completedMinutes: Int): List<GroveTree> {
        val total = completedMinutes.coerceAtLeast(0)
        val count = (total / PLANT_EVERY + 1).coerceAtMost(MAX_TREES)
        return (0 until count).map { k ->
            val m = total - k * PLANT_EVERY
            GroveTree(k, TreeStage.entries.last { m >= it.minutes }, m)
        }
    }

    /** What changed between two totals (for the session debrief). New seeds have from = null. */
    fun growth(before: Int, after: Int): List<TreeGrowth> {
        val a = compute(before).associateBy { it.index }
        return compute(after).mapNotNull { t ->
            val prev = a[t.index]?.stage
            if (prev != t.stage) TreeGrowth(t.index, prev, t.stage) else null
        }
    }
}
