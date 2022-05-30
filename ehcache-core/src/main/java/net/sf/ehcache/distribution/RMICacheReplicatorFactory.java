/**
 * Copyright Terracotta, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</a>
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sf.ehcache.distribution;

import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerFactory;
import net.sf.ehcache.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static net.sf.ehcache.util.PropertyUtil.extractBoolean;


/**
 * Creates an RMICacheReplicator using properties. Config lines look like:
 * <pre>&lt;cacheEventListenerFactory class="net.sf.ehcache.distribution.RMICacheReplicatorFactory"
 * properties="
 * replicateAsynchronously=true,
 * replicatePuts=true
 * replicateUpdates=true
 * replicateUpdatesViaCopy=true
 * replicateRemovals=true
 * "/&gt;</pre>
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class RMICacheReplicatorFactory extends CacheEventListenerFactory {

    /**
     * A default for the amount of time the replication thread sleeps after it detects the replicationQueue is empty
     * before checking again.
     */
    private static final int DEFAULT_ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS = 1000;

    /**
     * A default for the maximum number of operations in an RMI message.
     */
    private static final int DEFAULT_ASYNCHRONOUS_REPLICATION_MAXIMUM_BATCH_SIZE = 1000;

    /**
     * Minimum wait time.
     */
    private static final int MINIMUM_REASONABLE_INTERVAL = 10;

    private static final Logger LOG = LoggerFactory.getLogger(RMICacheReplicatorFactory.class);

    private static final String REPLICATE_PUTS = "replicatePuts";
    private static final String REPLICATE_PUTS_VIA_COPY = "replicatePutsViaCopy";
    private static final String REPLICATE_UPDATES = "replicateUpdates";
    private static final String REPLICATE_UPDATES_VIA_COPY = "replicateUpdatesViaCopy";
    private static final String REPLICATE_REMOVALS = "replicateRemovals";
    private static final String REPLICATE_ASYNCHRONOUSLY = "replicateAsynchronously";
    private static final String ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS = "asynchronousReplicationIntervalMillis";
    private static final String ASYNCHRONOUS_REPLICATION_MAXIMUM_BATCH_SIZE = "asynchronousReplicationMaximumBatchSize";

    /**
     * Create a <code>CacheEventListener</code> which is also a CacheReplicator.
     * <p>
     * The defaults if properties are not specified are:
     * <ul>
     * <li>replicatePuts=true
     * <li>replicatePutsViaCopy=true
     * <li>replicateUpdates=true
     * <li>replicateUpdatesViaCopy=true
     * <li>replicateRemovals=true;
     * <li>replicateAsynchronously=true
     * <li>asynchronousReplicationIntervalMillis=1000
     * </ul>
     *
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml e.g.
     *                   <p>
     *                   <code>
     *                   &lt;cacheEventListenerFactory class="net.sf.ehcache.distribution.RMICacheReplicatorFactory"
     *                   properties="
     *                   replicateAsynchronously=true,
     *                   replicatePuts=true
     *                   replicateUpdates=true
     *                   replicateUpdatesViaCopy=true
     *                   replicateRemovals=true
     *                   asynchronousReplicationIntervalMillis=1000
     *                   "/&gt;</code>
     * @return a constructed CacheEventListener
     */
    public final CacheEventListener createCacheEventListener(Properties properties) {

        boolean replicatePuts = extractBoolean(properties, REPLICATE_PUTS, true);
        boolean replicatePutsViaCopy = extractBoolean(properties, REPLICATE_PUTS_VIA_COPY, true);
        boolean replicateUpdates = extractBoolean(properties, REPLICATE_UPDATES, true);
        boolean replicateUpdatesViaCopy = extractBoolean(properties, REPLICATE_UPDATES_VIA_COPY, true);
        boolean replicateRemovals = extractBoolean(properties, REPLICATE_REMOVALS, true);
        boolean replicateAsynchronously = extractBoolean(properties, REPLICATE_ASYNCHRONOUSLY, true);

        if (replicateAsynchronously) {
            int replicationIntervalMillis = extractReplicationIntervalMilis(properties);
            int maximumBatchSize = extractMaximumBatchSize(properties);

            return new RMIAsynchronousCacheReplicator(
                    replicatePuts,
                    replicatePutsViaCopy,
                    replicateUpdates,
                    replicateUpdatesViaCopy,
                    replicateRemovals,
                    replicationIntervalMillis,
                    maximumBatchSize);
        } else {
            return new RMISynchronousCacheReplicator(
                    replicatePuts,
                    replicatePutsViaCopy,
                    replicateUpdates,
                    replicateUpdatesViaCopy,
                    replicateRemovals);
        }
    }

    /**
     * Extracts the value of asynchronousReplicationIntervalMillis. Sets it to 1000ms if
     * either not set or there is a problem parsing the number
     */
    protected int extractReplicationIntervalMilis(Properties properties) {
        String asynchronousReplicationIntervalMillisString =
                PropertyUtil.extractString(properties, ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS, "" + DEFAULT_ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS);
        try {
            int asynchronousReplicationIntervalMillis = Integer.parseInt(asynchronousReplicationIntervalMillisString);
            if (asynchronousReplicationIntervalMillis < MINIMUM_REASONABLE_INTERVAL) {
                LOG.debug("Trying to set the asynchronousReplicationIntervalMillis to an unreasonable number." +
                        " Using the default {}ms instead.", DEFAULT_ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS);
                return DEFAULT_ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS;
            } else {
                return asynchronousReplicationIntervalMillis;
            }
        } catch (NumberFormatException e) {
            LOG.warn("Number format exception trying to set asynchronousReplicationIntervalMillis. " +
                    "Using the default instead. String value was: '" + asynchronousReplicationIntervalMillisString + "'");
            return DEFAULT_ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS;
        }
    }

    /**
     * Extracts the value of maximumBatchSize. Sets it to 1024 if
     * either not set or there is a problem parsing the number
     */
    protected int extractMaximumBatchSize(Properties properties) {
        String maximumBatchSizeString = PropertyUtil.extractString(properties, ASYNCHRONOUS_REPLICATION_MAXIMUM_BATCH_SIZE, "" + DEFAULT_ASYNCHRONOUS_REPLICATION_MAXIMUM_BATCH_SIZE);
        try {
            return Integer.parseInt(maximumBatchSizeString);
        } catch (NumberFormatException e) {
            LOG.warn("Number format exception trying to set maximumBatchSize. " +
                    "Using the default instead. String value was: '" + maximumBatchSizeString + "'");
            return DEFAULT_ASYNCHRONOUS_REPLICATION_MAXIMUM_BATCH_SIZE;
        }
    }

}
