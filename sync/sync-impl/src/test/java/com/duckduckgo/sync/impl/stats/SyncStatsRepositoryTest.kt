/*
 * Copyright (c) 2023 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.sync.impl.stats

import com.duckduckgo.sync.impl.engine.SyncStateRepository
import com.duckduckgo.sync.store.model.SyncAttempt
import com.duckduckgo.sync.store.model.SyncAttemptState.FAIL
import com.duckduckgo.sync.store.model.SyncAttemptState.SUCCESS
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SyncStatsRepositoryTest {

    private var syncStateRepository: SyncStateRepository = mock()

    private lateinit var repository: SyncStatsRepository

    @Before
    fun setup() {
        repository = RealSyncStatsRepository(syncStateRepository)
    }

    @Test
    fun whenNoAttemptsThenDailyStatsIsEmpty() {
        whenever(syncStateRepository.attempts()).thenReturn(emptyList())

        val stats = repository.getDailyStats()

        assertTrue(stats.attempts == 0)
        assertTrue(stats.successRate == 0.00)
    }

    @Test
    fun whenOnlyPastAttemptsThenDailyStatsHasCorrectData() {
        val lastSyncTimestamp = timestamp(Instant.now().minus(5, ChronoUnit.DAYS))
        val lastSync = SyncAttempt(timestamp = lastSyncTimestamp, state = SUCCESS)

        whenever(syncStateRepository.attempts()).thenReturn(listOf(lastSync))

        val stats = repository.getDailyStats()

        assertTrue(stats.attempts == 0)
    }

    @Test
    fun whenOnlyYesterdayAttemptsThenDailyStatsHasCorrectData() {
        val lastSyncTimestamp = timestamp(Instant.now().minus(1, ChronoUnit.DAYS))
        val lastSync = SyncAttempt(timestamp = lastSyncTimestamp, state = SUCCESS)

        whenever(syncStateRepository.attempts()).thenReturn(listOf(lastSync))

        val stats = repository.getDailyStats()

        assertTrue(stats.attempts == 1)
    }

    @Test
    fun whenOnlyTodayAttemptsThenDailyStatsHasCorrectData() {
        val lastSyncTimestamp = timestamp(Instant.now().minus(5, ChronoUnit.MINUTES))
        val lastSync = SyncAttempt(timestamp = lastSyncTimestamp, state = SUCCESS)

        whenever(syncStateRepository.attempts()).thenReturn(listOf(lastSync))

        val stats = repository.getDailyStats()

        assertTrue(stats.attempts == 0)
    }

    @Test
    fun whenOnlySuccessfulAttemptsThenDailyStatsHasCorrectData() {
        val lastSyncTimestamp = timestamp(Instant.now().minus(1, ChronoUnit.DAYS))
        val lastSync = SyncAttempt(timestamp = lastSyncTimestamp, state = SUCCESS)

        whenever(syncStateRepository.attempts()).thenReturn(listOf(lastSync))

        val stats = repository.getDailyStats()

        assertTrue(stats.attempts == 1)
        assertTrue(stats.successRate == 100.0)
    }

    @Test
    fun whenOnlyFailedAttemptsThenDailyStatsHasCorrectData() {
        val lastSyncTimestamp = timestamp(Instant.now().minus(1, ChronoUnit.DAYS))
        val lastSync = SyncAttempt(timestamp = lastSyncTimestamp, state = FAIL)

        whenever(syncStateRepository.attempts()).thenReturn(listOf(lastSync))

        val stats = repository.getDailyStats()

        assertTrue(stats.attempts == 1)
        assertTrue(stats.successRate == 0.00)
    }

    private fun timestamp(instant: Instant): String {
        return instant.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
    }

    @Test
    fun whenFewAttemptsThenDailyStatsHasCorrectData() {
        val lastSyncTimestamp = timestamp(Instant.now().minus(1, ChronoUnit.DAYS))
        val first = SyncAttempt(timestamp = lastSyncTimestamp, state = FAIL)
        val second = SyncAttempt(timestamp = lastSyncTimestamp, state = SUCCESS)
        val third = SyncAttempt(timestamp = lastSyncTimestamp, state = FAIL)
        val fourth = SyncAttempt(timestamp = lastSyncTimestamp, state = SUCCESS)
        val fifth = SyncAttempt(timestamp = lastSyncTimestamp, state = SUCCESS)
        val sixth = SyncAttempt(timestamp = lastSyncTimestamp, state = SUCCESS)

        whenever(syncStateRepository.attempts()).thenReturn(listOf(first, second, third, fourth, fifth, sixth))

        val stats = repository.getDailyStats()

        assertTrue(stats.attempts == 6)
        assertTrue(stats.successRate == 66.67)
    }
}
