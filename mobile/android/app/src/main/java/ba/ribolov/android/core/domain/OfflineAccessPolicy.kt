package ba.ribolov.android.core.domain

import java.time.Duration

enum class OfflineAccess { FRESH, STALE, EXPIRED, REQUIRES_SYNC }

/**
 * Evaluates trusted elapsed time, never wall-clock differences.
 * reboot/clock anomalies must set trustedClock=false in the platform adapter.
 * Limits reflect proposed ODL-06 and must be confirmed before the pilot.
 */
class OfflineAccessPolicy(
    private val freshFor: Duration = Duration.ofHours(24),
    private val maximumGrant: Duration = Duration.ofHours(72),
) {
    init {
        require(!freshFor.isNegative && !freshFor.isZero)
        require(maximumGrant >= freshFor)
    }

    fun evaluate(
        elapsed: Duration?,
        trustedClock: Boolean,
        grantValidFor: Duration = maximumGrant,
    ): OfflineAccess {
        if (!trustedClock || elapsed == null || elapsed.isNegative) return OfflineAccess.REQUIRES_SYNC
        val expiry = minOf(maximumGrant, grantValidFor)
        if (expiry.isNegative || expiry.isZero || elapsed >= expiry) return OfflineAccess.EXPIRED
        return if (elapsed < freshFor) OfflineAccess.FRESH else OfflineAccess.STALE
    }
}