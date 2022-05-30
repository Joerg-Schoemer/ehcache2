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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;
import java.util.StringTokenizer;

import static net.sf.ehcache.util.PropertyUtil.extractString;
import static net.sf.ehcache.util.PropertyUtil.extractInt;

/**
 * Builds a factory based on RMI
 *
 * @author Greg Luck
 * @version $Id$
 */
public class RMICacheManagerPeerProviderFactory extends CacheManagerPeerProviderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(RMICacheManagerPeerProviderFactory.class);

    private static final String HOST_NAME = "hostName";
    private static final String PEER_DISCOVERY = "peerDiscovery";
    private static final String AUTOMATIC_PEER_DISCOVERY = "automatic";
    private static final String MANUALLY_CONFIGURED_PEER_DISCOVERY = "manual";
    private static final String RMI_URLS = "rmiUrls";
    private static final String MULTICAST_GROUP_PORT = "multicastGroupPort";
    private static final String MULTICAST_GROUP_ADDRESS = "multicastGroupAddress";
    private static final String MULTICAST_PACKET_TTL = "timeToLive";
    private static final int MAXIMUM_TTL = 255;


    /**
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml
     */
    public CacheManagerPeerProvider createCachePeerProvider(CacheManager cacheManager, Properties properties)
            throws CacheException {
        String peerDiscovery = PropertyUtil.extractString(properties, PEER_DISCOVERY, AUTOMATIC_PEER_DISCOVERY);
        if (AUTOMATIC_PEER_DISCOVERY.equalsIgnoreCase(peerDiscovery)) {
            try {
                return createMulticastPeerProvider(cacheManager, properties);
            } catch (IOException e) {
                throw new CacheException("Could not create CacheManagerPeerProvider.", e);
            }
        } else if (MANUALLY_CONFIGURED_PEER_DISCOVERY.equalsIgnoreCase(peerDiscovery)) {
            return createManualPeerProvider(properties, cacheManager);
        } else {
            return null;
        }
    }


    /**
     * peerDiscovery=manual, rmiUrls=//hostname:port/cacheName //hostname:port/cacheName //hostname:port/cacheName
     */
    CacheManagerPeerProvider createManualPeerProvider(Properties properties, CacheManager cacheManager) {
        String rmiUrls = PropertyUtil.extractString(properties, RMI_URLS, "");
        if (rmiUrls.isEmpty()) {
            LOG.warn("Starting manual peer provider with empty list of peers. No replication will occur unless peers are added.");
        }
        StringTokenizer stringTokenizer = new StringTokenizer(rmiUrls, PayloadUtil.URL_DELIMITER);
        CacheManagerPeerProvider rmiPeerProvider = new ManualRMICacheManagerPeerProvider(cacheManager);
        while (stringTokenizer.hasMoreTokens()) {
            String rmiUrl = stringTokenizer.nextToken().trim();
            rmiPeerProvider.registerPeer(rmiUrl);

            LOG.debug("Registering peer {}", rmiUrl);
        }

        return rmiPeerProvider;
    }

    /**
     * peerDiscovery=automatic, multicastGroupAddress=230.0.0.1, multicastGroupPort=4446, multicastPacketTimeToLive=255
     */
    CacheManagerPeerProvider createMulticastPeerProvider(CacheManager cacheManager, Properties properties) throws IOException {

        String hostName = extractString(properties, HOST_NAME);
        InetAddress hostAddress = null;
        if (hostName != null && hostName.length() != 0) {
            hostAddress = InetAddress.getByName(hostName);
            if ("localhost".equals(hostName)) {
                LOG.warn("Explicitly setting the multicast hostname to 'localhost' is not recommended. It will only work if all CacheManager peers are on the same machine.");
            }
        }

        InetAddress groupAddress = InetAddress.getByName(extractString(properties, MULTICAST_GROUP_ADDRESS));
        Integer multicastPort = Integer.valueOf(extractString(properties, MULTICAST_GROUP_PORT));

        int timeToLive = extractInt(properties, MULTICAST_PACKET_TTL, 1);
        if (0 > timeToLive || timeToLive > MAXIMUM_TTL) {
            throw new CacheException("The TTL must be set to a value between 0 and 255");
        }

        return new MulticastRMICacheManagerPeerProvider(cacheManager, groupAddress, multicastPort, timeToLive, hostAddress);
    }
}
