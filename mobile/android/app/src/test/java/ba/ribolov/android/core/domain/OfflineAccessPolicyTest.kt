package ba.ribolov.android.core.domain

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineAccessPolicyTest {
    private val policy = OfflineAccessPolicy()

    @Test fun boundariesDoNotExtendFreshnessOrAuthorization() {
        assertEquals(OfflineAccess.FRESH, policy.evaluate(Duration.ofHours(24).minusNanos(1), true))
        assertEquals(OfflineAccess.STALE, policy.evaluate(Duration.ofHours(24), true))
        assertEquals(OfflineAccess.EXPIRED, policy.evaluate(Duration.ofHours(72), true))
    }

    @Test fun untrustedOrMissingTimeRequiresOnlineRenewal() {
        assertEquals(OfflineAccess.REQUIRES_SYNC, policy.evaluate(Duration.ofHours(1), false))
        assertEquals(OfflineAccess.REQUIRES_SYNC, policy.evaluate(null, true))
        assertEquals(OfflineAccess.REQUIRES_SYNC, policy.evaluate(Duration.ofSeconds(-1), true))
    }

    @Test fun territorialAuthorizationMayExpireBeforeTheDefaultGrant() {
        assertEquals(OfflineAccess.EXPIRED, policy.evaluate(Duration.ofHours(3), true, Duration.ofHours(2)))
    }
}