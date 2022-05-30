/**
 * Copyright Terracotta, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</a>
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sf.ehcache.distribution;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;

import java.net.UnknownHostException;
import java.util.Properties;

import static net.sf.ehcache.util.PropertyUtil.extractString;
import static net.sf.ehcache.util.PropertyUtil.extractInt;

/**
 * Builds a listener based on RMI.
 * <p>
 * Expected configuration line:
 * <p>
 * <code>
 * &lt;cachePeerListenerFactory class="net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory"
 * properties="hostName=localhost, port=5000" /&gt;
 * </code>
 *
 * @author Greg Luck
 * @version $Id$
 */
public class RMICacheManagerPeerListenerFactory extends CacheManagerPeerListenerFactory {

    /**
     * The default timeout for cache replication for a single replication action.
     * This may need to be increased for large data transfers.
     */
    private static final Integer DEFAULT_SOCKET_TIMEOUT_MILLIS = 120000;

    private static final String HOSTNAME = "hostName";
    private static final String PORT = "port";
    private static final String REMOTE_OBJECT_PORT = "remoteObjectPort";
    private static final String SOCKET_TIMEOUT_MILLIS = "socketTimeoutMillis";

    /**
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml
     */
    public final CacheManagerPeerListener createCachePeerListener(CacheManager cacheManager, Properties properties)
            throws CacheException {

        String hostName = extractString(properties, HOSTNAME);
        int port = extractInt(properties, PORT, 0);
        //0 means any port in UnicastRemoteObject, so it is ok if not specified to make it 0
        int remoteObjectPort = extractInt(properties, REMOTE_OBJECT_PORT, 0);
        int socketTimeoutMillis = extractInt(properties, SOCKET_TIMEOUT_MILLIS, DEFAULT_SOCKET_TIMEOUT_MILLIS);

        return doCreateCachePeerListener(hostName, port, remoteObjectPort, socketTimeoutMillis, cacheManager);
    }

    /**
     * A template method to actually create the factory
     *
     * @param socketTimeoutMillis @return a crate CacheManagerPeerListener
     */
    protected CacheManagerPeerListener doCreateCachePeerListener(
            String hostName,
            int port,
            int remoteObjectPort,
            int socketTimeoutMillis, CacheManager cacheManager
    ) {
        try {
            return new RMICacheManagerPeerListener(hostName, port, remoteObjectPort, cacheManager, socketTimeoutMillis);
        } catch (UnknownHostException e) {
            throw new CacheException("Unable to create CacheManagerPeerListener.", e);
        }
    }
}
