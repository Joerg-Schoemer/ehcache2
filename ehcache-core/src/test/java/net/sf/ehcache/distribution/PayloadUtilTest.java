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

import net.sf.ehcache.Element;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.terracotta.test.categories.CheckShorts;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Note these tests need a live network interface running in multicast mode to work
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
@Category(CheckShorts.class)
public class PayloadUtilTest {

    private static final Random RANDOM = new SecureRandom();
    private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.";

    @Test
    public void createCompressedPayloadWithOneUrl() {
        List<CachePeer> list = singletonList(new PayloadUtilTestCachePeer("hugo"));

        List<byte[]> compressedPayload = PayloadUtil.createCompressedPayload(list, PayloadUtil.MTU);

        assertThat(compressedPayload).hasSize(1);
    }

    @Test
    public void createCompressedPayloadListWith10PeersAndOnly5UrlsPerSend() {
        List<CachePeer> list = IntStream.range(1, 11)
                .mapToObj(i -> new PayloadUtilTestCachePeer("Cache " + (Integer) i))
                .collect(Collectors.toList());

        List<byte[]> compressedPayload = PayloadUtil.createCompressedPayloadList(list, 5);

        assertThat(compressedPayload).hasSize(2);
    }

    @Test
    public void createCompressedPayloadWithToMuchUrlsForOneDatagram() {
        List<CachePeer> list = IntStream.range(1, 1000)
                .mapToObj(i -> new PayloadUtilTestCachePeer("Cache" + i))
                .collect(Collectors.toList());

        List<byte[]> compressedPayload = PayloadUtil.createCompressedPayload(list, 1500);

        assertThat(compressedPayload)
                .hasSize(2)
                .map(i -> i.length)
                .containsExactly(1246, 1235);
    }

    @Test
    public void heartbeatWontWork() {
        List<CachePeer> peerList = singletonList(new PayloadUtilTestCachePeer(getRandomName(3000)));
        List<byte[]> compressedList = PayloadUtil.createCompressedPayload(peerList, PayloadUtil.MTU);

        assertThat(compressedList).isEmpty();
    }

    @Test
    public void unzip() throws Exception {
        byte[] compressed = PayloadUtil.gzip("Test".getBytes());
        byte[] ungzip = PayloadUtil.ungzip(compressed);

        assertThat(new String(ungzip)).isEqualTo("Test");
    }

    @Test
    public void unzipWithMoreData() throws Exception {
        byte[] bytes = new byte[PayloadUtil.MTU * 2];
        new SecureRandom().nextBytes(bytes);
        byte[] compressed = PayloadUtil.gzip(bytes);
        byte[] ungzip = PayloadUtil.ungzip(compressed);

        assertThat(ungzip).isEqualTo(bytes);
    }



    @Test
    public void getUrlWillThrowRemoteException() throws RemoteException {
        CachePeer mock = Mockito.mock(CachePeer.class);
        given(mock.getUrl()).willThrow(RemoteException.class);

        List<byte[]> compressedPayload = PayloadUtil.createCompressedPayload(singletonList(mock), 10);
        assertThat(compressedPayload).isEmpty();
    }

    @SuppressWarnings("SameParameterValue")
    String getRandomName(int length) {
        StringBuilder rv = new StringBuilder();
        for (int i = 0; i < length; i++) {
            rv.append(RANDOM_CHARS.charAt(RANDOM.nextInt(RANDOM_CHARS.length())));
        }
        return rv.toString();
    }

    /**
     * A test class which implements only {@link #getUrl()} to test PayloadUtil.createCompressedPayloadList()
     *
     * @author Abhishek Sanoujam
     */
    private static class PayloadUtilTestCachePeer implements CachePeer {

        public static final String URL_BASE = "//localhost.localdomain:12000/";
        private final String cacheName;

        public PayloadUtilTestCachePeer(String cacheName) {
            this.cacheName = cacheName;
        }

        /**
         * {@inheritDoc}
         *
         * @see net.sf.ehcache.distribution.CachePeer#getUrl()
         */
        public String getUrl() throws RemoteException {
            return URL_BASE + cacheName;
        }

        /**
         * {@inheritDoc}
         *
         * @see net.sf.ehcache.distribution.CachePeer#getElements(java.util.List)
         */
        public List<Element> getElements(List<Serializable> keys) throws RemoteException {
            // no-op
            return null;
        }

        /**
         * {@inheritDoc}
         *
         * @see net.sf.ehcache.distribution.CachePeer#getGuid()
         */
        public String getGuid() throws RemoteException {
            // no-op
            return null;
        }

        /**
         * {@inheritDoc}
         *
         * @see net.sf.ehcache.distribution.CachePeer#getKeys()
         */
        public List<Serializable> getKeys() throws RemoteException {
            // no-op
            return null;
        }

        /**
         * {@inheritDoc}
         *
         * @see net.sf.ehcache.distribution.CachePeer#getName()
         */
        public String getName() throws RemoteException {
            // no-op
            return cacheName;
        }

        /**
         * {@inheritDoc}
         *
         * @see net.sf.ehcache.distribution.CachePeer#getQuiet(java.io.Serializable)
         */
        public Element getQuiet(Serializable key) throws RemoteException {
            // no-op
            return null;
        }

        /**
         * {@inheritDoc}
         *
         * @see net.sf.ehcache.distribution.CachePeer#getUrlBase()
         */
        public String getUrlBase() throws RemoteException {
            // no-op
            return null;
        }

        /**
         * {@inheritDoc}
         *
         * @see net.sf.ehcache.distribution.CachePeer#put(net.sf.ehcache.Element)
         */
        public void put(Element element) throws IllegalArgumentException, IllegalStateException, RemoteException {
            // no-op

        }

        /**
         * {@inheritDoc}
         *
         * @see net.sf.ehcache.distribution.CachePeer#remove(java.io.Serializable)
         */
        public boolean remove(Serializable key) throws IllegalStateException, RemoteException {
            // no-op
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * @see net.sf.ehcache.distribution.CachePeer#removeAll()
         */
        public void removeAll() throws RemoteException, IllegalStateException {
            // no-op
        }

        /**
         * {@inheritDoc}
         *
         * @see net.sf.ehcache.distribution.CachePeer#send(java.util.List)
         */
        public void send(List<RmiEventMessage> eventMessages) throws RemoteException {
            // no-op

        }

    }

}
