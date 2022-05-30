package net.sf.ehcache.distribution;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.terracotta.test.categories.CheckShorts;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Category(CheckShorts.class)
public class ManualRMICacheManagerPeerProviderTest {

    ManualRMICacheManagerPeerProvider sut = new ManualRMICacheManagerPeerProvider(null);

    @Test
    public void getRegisteredRmiUrls() {
        sut.peerUrls.add("//localhost/cacheName");

        Set<String> registeredRmiUrls = sut.getRegisteredRmiUrls();

        assertThat(registeredRmiUrls).hasSize(1).containsExactly("//localhost/cacheName");
    }

    @Test
    public void getTimeForClusterToForm() {
        assertThat(sut.getTimeForClusterToForm()).isZero();
    }

    @Test
    public void registerPeer() {
        sut.registerPeer("//localhost/cacheName");

        assertThat(sut.peerUrls).hasSize(1).containsExactly("//localhost/cacheName");
    }

    @Test
    public void unregisterPeer() {
        sut.peerUrls.add("//localhost/cacheName");

        sut.unregisterPeer("//localhost/cacheName");

        assertThat(sut.peerUrls).isEmpty();
    }
}