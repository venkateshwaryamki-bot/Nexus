package com.nexus.domain.time

/**
 * Time source abstraction. The focus engine never trusts a single clock:
 *
 * - [elapsedMillis] is monotonic since boot (Android `SystemClock.elapsedRealtime()`), includes deep
 *   sleep and cannot be changed by the user. It is the authority while the device stays on the same boot.
 * - [wallMillis] is the user-adjustable wall clock. It is only used to bridge a reboot (where the
 *   monotonic clock resets) and for calendar attribution (which day a session belongs to).
 * - [bootId] identifies the current boot (Android `Settings.Global.BOOT_COUNT`). A changed boot id means
 *   elapsed-time anchors from the previous boot are meaningless.
 */
interface Clock {
    fun wallMillis(): Long
    fun elapsedMillis(): Long
    fun bootId(): String
}

/** Mutable clock for tests and simulations. */
class FakeClock(
    var wall: Long = 1_700_000_000_000L,
    var elapsed: Long = 10_000L,
    var boot: String = "boot-1",
) : Clock {
    override fun wallMillis() = wall
    override fun elapsedMillis() = elapsed
    override fun bootId() = boot

    /** Real time passes: both clocks move together. */
    fun advance(ms: Long) { wall += ms; elapsed += ms }

    /** Simulates a reboot after [offMs] of downtime: monotonic clock restarts, wall clock keeps going. */
    fun reboot(offMs: Long, newBoot: String = boot + "'") {
        wall += offMs
        elapsed = 5_000L
        boot = newBoot
    }
}
