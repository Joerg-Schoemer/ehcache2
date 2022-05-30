package net.sf.ehcache.distribution;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.hibernate.cache.EhCache;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.test.categories.CheckShorts;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@Category(CheckShorts.class)
public class AbstractRMICacheManagerPeerProviderTest {

    CacheManager cacheManager = mock(CacheManager.class);

    Set<String> registry = new TreeSet<>();

    AbstractRMICacheManagerPeerProvider sut = new AbstractRMICacheManagerPeerProvider(cacheManager) {

        @Override
        Set<String> getRegisteredRmiUrls() {
            return new TreeSet<>(registry);
        }

        @Override
        public void registerPeer(String nodeId) {
            registry.add(nodeId);
        }

        @Override
        public void unregisterPeer(String nodeId) {
            registry.remove(nodeId);
        }

        @Override
        public long getTimeForClusterToForm() {
            return 4711;
        }
    };

    @Before
    public void setUp() throws Exception {
        registry.clear();
    }

    @Test
    public void extractCacheName() {
        String cacheName = AbstractRMICacheManagerPeerProvider.extractCacheName("//hostname/cacheName");

        assertThat(cacheName).isEqualTo("cacheName");
    }

    @Test
    public void lookupRemoteCachePeer() {
        String rmiUrl = "//localhost/cacheName";

        Optional<CachePeer> cachePeer = sut.lookupRemoteCachePeer(rmiUrl);

        assertThat(cachePeer).isNotPresent();
    }

    @Test
    public void getRegisteredRmiUrls() {
        registry.add("//hostname/cacheName1");
        registry.add("//hostname/cacheName2");

        Set<String> registeredRmiUrls = sut.getRegisteredRmiUrls();

        assertThat(registeredRmiUrls).hasSize(2);
    }

    @Test
    public void listRemoteCachePeers() {
        AbstractRMICacheManagerPeerProvider spy = spy(sut);
        String rmiUrl = "//localhost/cacheName";
        registry.add(rmiUrl);
        Ehcache cache = mock(Ehcache.class);
        given(cache.getName()).willReturn("cacheName");
        CachePeer cachePeer = mock(RMICachePeer.class);

        given(spy.lookupRemoteCachePeer(rmiUrl)).willReturn(Optional.of(cachePeer));

        List<CachePeer> cachePeers = spy.listRemoteCachePeers(cache);

        assertThat(cachePeers).hasSize(1).containsExactly(cachePeer);
    }

    @Test
    public void init() {
        assertThatNoException().isThrownBy(() -> sut.init());
    }

    @Test
    public void dispose() {
        assertThatNoException().isThrownBy(() -> sut.dispose());
    }

    @Test
    public void isSameCache() {
        boolean sameCache = sut.isSameCache("//remote/cacheName", "cacheName");

        assertThat(sameCache).isTrue();
    }

    @Test
    public void isNotSameCache() {
        boolean sameCache = sut.isSameCache("//remote/anotherCacheName", "cacheName");

        assertThat(sameCache).isFalse();
    }

    @Test
    public void getCachePeerListener() {
        CacheManagerPeerListener peerListener = mock(CacheManagerPeerListener.class);
        given(cacheManager.getCachePeerListener("RMI")).willReturn(peerListener);

        CacheManagerPeerListener cachePeerListener = sut.getCachePeerListener();
        assertThat(cachePeerListener).isEqualTo(peerListener);
    }

    @Test
    public void getScheme() {
        String scheme = sut.getScheme();
        assertThat(scheme).isEqualTo("RMI");
    }
}