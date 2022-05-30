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
import net.sf.ehcache.Ehcache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * A provider of Peer RMI addresses.
 *
 * @author Greg Luck
 * @version $Id$
 */
public abstract class AbstractRMICacheManagerPeerProvider implements CacheManagerPeerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRMICacheManagerPeerProvider.class);

    /**
     * The CacheManager this peer provider is associated with.
     */
    private final CacheManager cacheManager;


    /**
     * @param cacheManager The CacheManager to associate to for this provider.
     */
    public AbstractRMICacheManagerPeerProvider(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Gets the cache name out of the url
     * <p>
     *
     * @param rmiUrl the URL of the node.
     * @return the cache name as it would appear in ehcache.xml
     */
    static String extractCacheName(String rmiUrl) {
        return rmiUrl.substring(rmiUrl.lastIndexOf('/') + 1);
    }

    /**
     * The use of one-time registry creation and Naming.rebind should mean we can create as many listeneres as we like.
     * They will simply replace the ones that were there.
     *
     * @return an RMI Proxy or <code>null</code> if look-up fails.
     */
    Optional<CachePeer> lookupRemoteCachePeer(String url) {
        LOG.debug("Lookup URL {}", url);

        try {
            return Optional.of((CachePeer) Naming.lookup(url));
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            LOG.warn("could not lookup rmiUrl {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get a set of all registered rmi URLs.
     */
    abstract Set<String> getRegisteredRmiUrls();

    @Override
    public synchronized List<CachePeer> listRemoteCachePeers(Ehcache cache) throws CacheException {
        return getRegisteredRmiUrls().stream()
                .filter((rmiUrl) -> isSameCache(rmiUrl, cache.getName()))
                .map(this::lookupRemoteCachePeer)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public void init() {
        // nothing to do.
    }

    @Override
    public void dispose() {
        //nothing to do.
    }

    /**
     * Checks whether the URL contains the same cache name.
     */
    boolean isSameCache(String rmiUrl, String cacheName) {
        String rmiUrlCacheName = extractCacheName(rmiUrl);
        boolean sameCacheName = rmiUrlCacheName.equals(cacheName);
        if (!sameCacheName) {
            LOG.trace("cache name differ {} <> {}", rmiUrlCacheName, cacheName);
        }

        return sameCacheName;
    }

    public CacheManagerPeerListener getCachePeerListener() {
        return cacheManager.getCachePeerListener(getScheme());
    }

    /**
     * The replication scheme. Each peer provider has a scheme name, which can be used to specify
     * the scheme for replication and bootstrap purposes. Each <code>CacheReplicator</code> should lookup
     * the provider for its scheme type during replication. Similarly a <code>BootstrapCacheLoader</code>
     * should also look up the provider for its scheme.
     * <p>
     *
     * @return the well-known scheme name, which is determined by the replication provider author.
     * @since 1.6 introduced to permit multiple distribution schemes to be used in the same CacheManager
     */
    public String getScheme() {
        return "RMI";
    }
}
