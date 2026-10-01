package com.owlcoder.animeschedule.data.provider

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private class FakeProviderClock(var now: Long = 0L) : ProviderClock {
    override fun nowMillis(): Long = now
}

class ProviderResilienceTest {
    @Test
    fun rateLimitOpensCircuitAndNextProviderIsUsed() = runTest {
        val clock = FakeProviderClock()
        val orchestrator = ProviderOrchestrator(ProviderHealthStore(clock), clock)
        var fallbackCalls = 0

        val first = orchestrator.firstSuccessful(
            ProviderOperation.SEARCH,
            listOf(
                ProviderCall<String>("Kitsu") {
                    throw ProviderCallException("Kitsu", statusCode = 429, retryAfterSeconds = 120)
                },
                ProviderCall("AnimeSchedule") {
                    fallbackCalls++
                    "fallback"
                }
            )
        )
        assertEquals(ProviderResult.Success("AnimeSchedule", "fallback"), first)

        val second = orchestrator.firstSuccessful(
            ProviderOperation.SEARCH,
            listOf(
                ProviderCall<String>("Kitsu") { "must not run" },
                ProviderCall("AnimeSchedule") { "fallback-2" }
            )
        )
        assertEquals(ProviderResult.Success("AnimeSchedule", "fallback-2"), second)
        assertEquals(1, fallbackCalls)
    }

    @Test
    fun threeTransientFailuresOpenCircuitForFiveMinutes() = runTest {
        val clock = FakeProviderClock()
        val store = ProviderHealthStore(clock)
        val orchestrator = ProviderOrchestrator(store, clock)
        repeat(3) {
            orchestrator.firstSuccessful(
                ProviderOperation.SEARCH,
                listOf(ProviderCall<String>("Kitsu") { throw IOException("offline") })
            )
        }

        assertTrue(store.isOpen("Kitsu"))
        assertFalse(store.tryAcquire("Kitsu"))
        clock.now += 5 * 60 * 1_000L
        assertTrue(store.tryAcquire("Kitsu"))
    }

    @Test
    fun unauthorizedProviderIsBlockedLongerThanTransientFailures() = runTest {
        val clock = FakeProviderClock()
        val store = ProviderHealthStore(clock)
        val orchestrator = ProviderOrchestrator(store, clock)

        orchestrator.firstSuccessful(
            ProviderOperation.SEARCH,
            listOf(ProviderCall<String>("AniList") {
                throw ProviderCallException("AniList", statusCode = 403)
            })
        )

        assertTrue(store.isOpen("AniList"))
        assertFalse(store.tryAcquire("AniList"))
        clock.now += 6 * 60 * 60 * 1_000L
        assertTrue(store.tryAcquire("AniList"))
    }

    @Test
    fun cancellationIsNeverConvertedToProviderFailure() = runTest {
        val clock = FakeProviderClock()
        val orchestrator = ProviderOrchestrator(ProviderHealthStore(clock), clock)
        var propagated = false

        try {
            orchestrator.firstSuccessful(
                ProviderOperation.DETAIL,
                listOf(ProviderCall<String>("AniList") {
                    throw CancellationException("screen closed")
                })
            )
        } catch (_: CancellationException) {
            propagated = true
        }

        assertTrue(propagated)
    }

    @Test
    fun cancelledRecoveryProbeDoesNotLeaveTheProviderBlockedForever() = runTest {
        val clock = FakeProviderClock()
        val store = ProviderHealthStore(clock)
        val orchestrator = ProviderOrchestrator(store, clock)
        repeat(3) {
            orchestrator.firstSuccessful(
                ProviderOperation.SEARCH,
                listOf(ProviderCall<String>("AniList") { throw IOException("offline") })
            )
        }
        clock.now += 5 * 60 * 1_000L

        // The first call after the cool-down is the recovery probe; the user navigates away
        // (e.g. a search debounce cancels it) before it completes.
        try {
            orchestrator.firstSuccessful(
                ProviderOperation.SEARCH,
                listOf(ProviderCall<String>("AniList") { throw CancellationException("cancelled") })
            )
        } catch (_: CancellationException) {
        }

        assertTrue("provider must be probeable again", store.tryAcquire("AniList"))
    }

    @Test
    fun probeIsReleasedWhenTheTimeBudgetIsAlreadySpent() = runTest {
        val clock = FakeProviderClock()
        val store = ProviderHealthStore(clock)
        val orchestrator = ProviderOrchestrator(store, clock)
        repeat(3) {
            orchestrator.firstSuccessful(
                ProviderOperation.SEARCH,
                listOf(ProviderCall<String>("Kitsu") { throw IOException("offline") })
            )
        }
        clock.now += 5 * 60 * 1_000L

        // The first provider burns the whole budget, so the half-open provider is never called.
        orchestrator.firstSuccessful(
            ProviderOperation.SEARCH,
            listOf(
                ProviderCall<String>("AniList") {
                    clock.now += ProviderOperation.SEARCH.totalBudgetMs
                    throw IOException("slow")
                },
                ProviderCall<String>("Kitsu") { "unused" }
            )
        )

        assertTrue("probe slot must not leak", store.tryAcquire("Kitsu"))
    }

    @Test
    fun callersOwnTimeoutIsNotBlamedOnTheProvider() = runTest {
        val clock = FakeProviderClock()
        val store = ProviderHealthStore(clock)
        val orchestrator = ProviderOrchestrator(store, clock)

        repeat(3) {
            // The caller gives up long before the provider's own 6s timeout would fire.
            val result = withTimeoutOrNull(100L) {
                orchestrator.firstSuccessful(
                    ProviderOperation.SCHEDULE,
                    listOf(ProviderCall<String>("AniList") {
                        delay(60_000L)
                        "too late"
                    })
                )
            }
            assertEquals(null, result)
        }

        assertFalse("caller timeouts must not trip the breaker", store.isOpen("AniList"))
    }
}
