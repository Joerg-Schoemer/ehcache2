/**
 *  Copyright 2003-2009 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.ehcache.statistics;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.util.counter.CounterManager;
import net.sf.ehcache.util.counter.CounterManagerImpl;
import net.sf.ehcache.util.counter.sampled.SampledCounter;
import net.sf.ehcache.util.counter.sampled.SampledCounterConfig;
import net.sf.ehcache.util.counter.sampled.SampledRateCounter;
import net.sf.ehcache.util.counter.sampled.SampledRateCounterConfig;

/**
 * An implementation of {@link SampledCacheUsageStatistics} This also implements
 * {@link CacheUsageListener} and depends on the notification received from
 * these to update the stats
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class SampledCacheUsageStatisticsImpl implements CacheUsageListener,
        SampledCacheUsageStatistics {

    private static final int DEFAULT_HISTORY_SIZE = 30;
    private static final int DEFAULT_INTERVAL_SECS = 3;
    private final CounterManager counterManager;
    private final SampledCounter cacheHitCount;
    private final SampledCounter cacheHitInMemoryCount;
    private final SampledCounter cacheHitOnDiskCount;
    private final SampledCounter cacheMissCount;
    private final SampledCounter cacheMissExpiredCount;
    private final SampledCounter cacheMissNotFoundCount;
    private final SampledCounter cacheElementEvictedCount;
    private final SampledCounter cacheElementRemoved;
    private final SampledCounter cacheElementExpired;
    private final SampledCounter cacheElementPut;
    private final SampledCounter cacheElementUpdated;
    private final SampledRateCounter averageGetTime;

    private final AtomicBoolean statisticsEnabled;
    private final AtomicInteger statisticsAccuracy;

    /**
     * Default Constructor
     */
    public SampledCacheUsageStatisticsImpl() {
        counterManager = new CounterManagerImpl();
        SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(
                DEFAULT_INTERVAL_SECS, DEFAULT_HISTORY_SIZE, true, 0L);
        cacheHitCount = (SampledCounter) counterManager
                .createCounter(sampledCounterConfig);
        cacheHitInMemoryCount = (SampledCounter) counterManager
                .createCounter(sampledCounterConfig);
        cacheHitOnDiskCount = (SampledCounter) counterManager
                .createCounter(sampledCounterConfig);
        cacheMissCount = (SampledCounter) counterManager
                .createCounter(sampledCounterConfig);
        cacheMissExpiredCount = (SampledCounter) counterManager
                .createCounter(sampledCounterConfig);
        cacheMissNotFoundCount = (SampledCounter) counterManager
                .createCounter(sampledCounterConfig);
        cacheElementEvictedCount = (SampledCounter) counterManager
                .createCounter(sampledCounterConfig);
        cacheElementRemoved = (SampledCounter) counterManager
                .createCounter(sampledCounterConfig);
        cacheElementExpired = (SampledCounter) counterManager
                .createCounter(sampledCounterConfig);
        cacheElementPut = (SampledCounter) counterManager
                .createCounter(sampledCounterConfig);
        cacheElementUpdated = (SampledCounter) counterManager
                .createCounter(sampledCounterConfig);

        final SampledRateCounterConfig sampledRateCounterConfig = new SampledRateCounterConfig(
                3, 30, true);
        averageGetTime = (SampledRateCounter) counterManager
                .createCounter(sampledRateCounterConfig);

        statisticsEnabled = new AtomicBoolean(true);
        statisticsAccuracy = new AtomicInteger();
    }

    private void incrementIfStatsEnabled(SampledCounter... counters) {
        if (!statisticsEnabled.get()) {
            return;
        }
        for (SampledCounter counter : counters) {
            counter.increment();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheElementEvicted() {
        incrementIfStatsEnabled(cacheElementEvictedCount);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheHitInMemory() {
        incrementIfStatsEnabled(cacheHitCount, cacheHitInMemoryCount);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheHitOnDisk() {
        incrementIfStatsEnabled(cacheHitCount, cacheHitOnDiskCount);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheMissedWithExpired() {
        incrementIfStatsEnabled(cacheMissCount, cacheMissExpiredCount);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheMissedWithNotFound() {
        incrementIfStatsEnabled(cacheMissCount, cacheMissNotFoundCount);
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        counterManager.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheElementExpired() {
        incrementIfStatsEnabled(cacheElementExpired);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheElementPut() throws CacheException {
        incrementIfStatsEnabled(cacheElementPut);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheElementRemoved() throws CacheException {
        incrementIfStatsEnabled(cacheElementRemoved);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheElementUpdated() throws CacheException {
        incrementIfStatsEnabled(cacheElementUpdated);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyTimeTakenForGet(long millis) {
        if (!statisticsEnabled.get()) {
            return;
        }
        averageGetTime.increment(millis, 1);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyRemoveAll() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyStatisticsEnabledChanged(boolean enableStatistics) {
        statisticsEnabled.set(enableStatistics);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyStatisticsAccuracyChanged(int statisticsAccuracyValue) {
        statisticsAccuracy.set(statisticsAccuracyValue);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyStatisticsCleared() {
        cacheHitCount.getAndReset();
        cacheHitInMemoryCount.getAndReset();
        cacheHitOnDiskCount.getAndReset();
        cacheMissCount.getAndReset();
        cacheMissExpiredCount.getAndReset();
        cacheMissNotFoundCount.getAndReset();
        cacheElementEvictedCount.getAndReset();
        averageGetTime.getAndReset();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitMostRecentSample() {
        return cacheHitCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getAverageGetTimeMostRecentSample() {
        return averageGetTime.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementEvictedMostRecentSample() {
        return cacheElementEvictedCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitInMemoryMostRecentSample() {
        return cacheHitInMemoryCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitOnDiskMostRecentSample() {
        return cacheHitOnDiskCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissExpiredMostRecentSample() {
        return cacheMissExpiredCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissMostRecentSample() {
        return cacheMissCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissNotFoundMostRecentSample() {
        return cacheMissNotFoundCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementExpiredMostRecentSample() {
        return cacheElementExpired.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementPutMostRecentSample() {
        return cacheElementPut.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementRemovedMostRecentSample() {
        return cacheElementRemoved.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementUpdatedMostRecentSample() {
        return cacheElementUpdated.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public int getStatisticsAccuracy() {
        return statisticsAccuracy.get();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStatisticsEnabled() {
        return statisticsEnabled.get();
    }

}
