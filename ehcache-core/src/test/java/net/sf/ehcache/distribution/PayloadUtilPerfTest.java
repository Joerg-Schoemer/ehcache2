package net.sf.ehcache.distribution;

import net.sf.ehcache.StopWatch;
import net.sf.ehcache.config.Configuration;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;
import static net.sf.ehcache.config.ConfigurationFactory.parseConfiguration;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alex Snaps
 */
public class PayloadUtilPerfTest {

    @Test
    public void testGzipSanityAndPerformance() {
        byte[] payloadBytes = createReferenceString().getBytes();

        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 1_000; i++) {
            PayloadUtil.gzip(payloadBytes);
        }

        long elapsedTime = stopWatch.getElapsedTime();

        assertThat(elapsedTime).isLessThan(500L);
    }

    @Test
    public void referenceStringIsAscii() {
        String payload = createReferenceString();
        byte[] original = payload.getBytes(StandardCharsets.UTF_8);

        assertThat(payload.toCharArray().length).isEqualTo(original.length);
    }

    @Test
    public void testUngzipPerformance() throws IOException {

        byte[] compressed = PayloadUtil.gzip(createReferenceString().getBytes());

        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 1_000; i++) {
            PayloadUtil.ungzip(compressed);
        }
        long elapsed = stopWatch.getElapsedTime();

        assertThat(elapsed).isLessThan(500);
    }

    private String createReferenceString() {
        String resourceName = "/cachemanager-perf.xml";
        URL resource = requireNonNull(
                PayloadUtilPerfTest.class.getResource(resourceName),
                resourceName + " not found");
        Configuration config = parseConfiguration(resource);
        StringBuilder buffer = new StringBuilder();
        for (String name : config.getCacheConfigurations().keySet()) {
            if (buffer.length() > 0) {
                buffer.append("|");
            }
            buffer.append("//localhost.localdomain:12000/");
            buffer.append(name);
        }

        return buffer.toString();
    }

}
