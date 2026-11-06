package com.nexus.domain.progression

enum class Rank(val title: String, val minLevel: Int) {
    RECRUIT("Recruit", 1),
    EXPLORER("Explorer", 5),
    COMMANDER("Commander", 10),
    PIONEER("Pioneer", 20),
    GALACTIC_MASTER("Galactic Master", 30),
    NEXUS_LEGEND("Nexus Legend", 45);

    companion object {
        fun forLevel(level: Int): Rank = entries.last { level >= it.minLevel }
    }
}

data class LevelInfo(
    val level: Int,
    val rank: Rank,
    val xpIntoLevel: Long,
    val xpForNextLevel: Long,
) {
    val progress: Float get() = if (xpForNextLevel == 0L) 1f else xpIntoLevel.toFloat() / xpForNextLevel
}

/**
 * XP curve: reaching level L+1 from L costs 100 + 50·(L-1) XP.
 * Cumulative XP to reach level L = 100·(L-1) + 25·(L-1)·(L-2).
 * A 25-minute completed session gives 75 XP, so level 5 takes ~10 sessions and level 45 several months.
 */
object Progression {
    const val MAX_LEVEL = 99

    fun totalXpForLevel(level: Int): Long {
        val n = (level - 1).toLong()
        return 100 * n + 25 * n * (n - 1)
    }

    fun levelFor(xp: Long): LevelInfo {
        val totalXp = xp.coerceAtLeast(0)
        var level = 1
        while (level < MAX_LEVEL && totalXpForLevel(level + 1) <= totalXp) level++
        val base = totalXpForLevel(level)
        val next = if (level == MAX_LEVEL) 0 else totalXpForLevel(level + 1) - base
        return LevelInfo(level, Rank.forLevel(level), totalXp - base, next)
    }
}
