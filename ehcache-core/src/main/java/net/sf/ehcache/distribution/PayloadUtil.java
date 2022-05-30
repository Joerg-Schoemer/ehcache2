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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static javax.xml.bind.DatatypeConverter.printHexBinary;

/**
 * This class provides utility methods for assembling and disassembling a heartbeat payload.
 * <p>
 * Care is taken to fit the payload into the MTU of ethernet, which is 1500 bytes. The algorithms in this class are capable of creating
 * payloads for CacheManagers containing approximately 500 cache peers to be replicated.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
class PayloadUtil {

    /**
     * The maximum transmission unit. This varies by link layer. For ethernet, fast ethernet and
     * gigabit ethernet it is 1500 bytes, the value chosen.
     * <p>
     * Payloads are limited to this so that there is no fragmentation and no necessity for a complex reassembly protocol.
     */
    public static final int MTU = 1500;

    /**
     * Delmits URLS sent via heartbeats over sockets
     */
    public static final String URL_DELIMITER = "|";

    /**
     * {@link #URL_DELIMITER} as a regular expression. Package protected, used in tests only
     */
    static final String URL_DELIMITER_REGEXP = "\\|";

    private static final Logger LOG = LoggerFactory.getLogger(PayloadUtil.class);

    /**
     * Utility class therefore precent construction
     */
    private PayloadUtil() {
        // noop
    }

    /**
     * Creates a list of compressed (using gzip) url list. Breaks up the list of urlList such that size of each compressed entry in the list
     * does not exceed the {@link #MTU} and the number of url's in each compressed entry does not exceed the maximumPeersPerSend parameter
     *
     * @param localCachePeers     List containing the peers
     * @param maximumPeersPerSend The maximum number of peers that can be present in one compressed entry
     * @return List of compressed entries containing the peers urlList
     */
    public static List<byte[]> createCompressedPayloadList(final List<CachePeer> localCachePeers, final int maximumPeersPerSend) {
        List<byte[]> rv = new ArrayList<>();
        int chunks = (int) ceil((double) localCachePeers.size() / maximumPeersPerSend);
        for (int i = 0; i < chunks; i++) {
            int fromIndex = maximumPeersPerSend * i;
            int toIndex = min(maximumPeersPerSend * (i + 1), localCachePeers.size());
            List<CachePeer> subList = localCachePeers.subList(fromIndex, toIndex);
            rv.addAll(createCompressedPayload(subList, MTU));
        }
        return rv;
    }

    /**
     * Generates a list of compressed urlList's for the input CachePeers list. Each compressed payload is limited by size by the
     * maxSizePerPayload parameter and will break up into multiple payloads if necessary to limit the payload size
     *
     * @param list              The list of CachePeers whose payload needs to be generated
     * @param maxSizePerPayload The maximum size each payload can have
     * @return A list of compressed urlList's, each compressed entry not exceeding maxSizePerPayload
     */
    static List<byte[]> createCompressedPayload(List<CachePeer> list, int maxSizePerPayload) {
        byte[] ungzipped = assembleUrlList(list);
        byte[] compressed = gzip(ungzipped);
        if (compressed.length <= maxSizePerPayload) {
            return Collections.singletonList(compressed);
        }

        // byte[] exceeds maxSizePerPayload, break up till we get under limit size

        if (1 == list.size()) {
            // only one cache, and the compressed size is bigger than MTU, must be some absurd very long cacheName
            String url = null;
            try {
                url = list.get(0).getUrl();
            } catch (RemoteException e) {
                LOG.error("This should never be thrown as it is called locally", e);
            }
            LOG.error("The replicated cache url is too long. Unless configured with a smaller name, " +
                    "heartbeat won't work for this cache. " +
                    "Compressed url size: " + compressed.length + " maxSizePerPayload: " + maxSizePerPayload + " URL: " + url);
            return Collections.emptyList();
        }

        List<byte[]> rv = new ArrayList<>();
        int halfSize = list.size() / 2;
        rv.addAll(createCompressedPayload(list.subList(0, halfSize), maxSizePerPayload));
        rv.addAll(createCompressedPayload(list.subList(halfSize, list.size()), maxSizePerPayload));
        return rv;
    }

    /**
     * Assembles a list of URLs
     *
     * @return an uncompressed payload with catenated rmiUrls.
     */
    public static byte[] assembleUrlList(List<CachePeer> localCachePeers) {
        StringBuilder sb = new StringBuilder();
        for (CachePeer cachePeer : localCachePeers) {
            try {
                if (sb.length() != 0) {
                    sb.append(URL_DELIMITER);
                }
                sb.append(cachePeer.getUrl());
            } catch (RemoteException e) {
                LOG.error("This should never be thrown as it is called locally", e);
            }
        }

        LOG.debug("Cache peers for this CacheManager to be advertised: {}", sb);
        return sb.toString().getBytes();
    }

    /**
     * Gzips a byte[]. For text, approximately 10:1 compression is achieved.
     *
     * @param ungzipped the bytes to be gzipped
     * @return gzipped bytes
     */
    public static byte[] gzip(byte[] ungzipped) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bytes)) {
            gzipOutputStream.write(ungzipped);
        } catch (IOException e) {
            LOG.error("Could not gzip " + printHexBinary(ungzipped));
        }
        return bytes.toByteArray();
    }

    /**
     * The fastest Ungzip implementation. See PageInfoTest in ehcache-constructs.
     * A high performance implementation, although not as fast as gunzip3.
     * gunzips 100000 of ungzipped content in 9ms on the reference machine.
     * It does not use a fixed size buffer and is therefore suitable for arbitrary
     * length arrays.
     *
     * @return a plain, uncompressed byte[]
     */
    public static byte[] ungzip(final byte[] gzipped) throws IOException {
        final byte[] buffer = new byte[PayloadUtil.MTU];
        try (final GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(gzipped))) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(buffer.length);
            int bytesRead;
            do {
                bytesRead = inputStream.read(buffer);
                if (bytesRead != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
            } while (bytesRead != -1);

            return byteArrayOutputStream.toByteArray();
        }
    }

}
