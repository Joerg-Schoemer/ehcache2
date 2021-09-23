/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is
 *      Terracotta, Inc., a Software AG company
 */
package org.terracotta.modules.ehcache.lifecycle;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.mockito.Mockito;
import org.objenesis.ObjenesisStd;
import net.bytebuddy.matcher.NullMatcher;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;
import com.terracotta.entity.ClusteredEntityManager;
import com.terracotta.entity.ehcache.ClusteredCacheManager;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class CacheManagerDestroyCrashTest extends AbstractCacheTestBase {
    private static final String CACHE_NAME = "cache1";

    public CacheManagerDestroyCrashTest(TestConfig testConfig) {
        super("lifecycle/cache-destroy.xml", testConfig, CacheManagerCreateClient.class,
                CacheManagerEntityDestroyCrashClient.class);
    }

    @Override
    protected String createClassPath(Class client) throws IOException {
        String classpath = super.createClassPath(client);
        classpath = addToClasspath(classpath, TestBaseUtil.jarFor(Mockito.class));
        classpath = addToClasspath(classpath, TestBaseUtil.jarFor(ObjenesisStd.class));
        classpath = addToClasspath(classpath, TestBaseUtil.jarFor(NullMatcher.class));
        return classpath;
    }

    public static class CacheManagerCreateClient extends ClientBase {

        public CacheManagerCreateClient(String[] mainArgs) {
            super(mainArgs);
        }

        @Override
        protected void runTest(Cache cache, Toolkit myToolkit) throws Throwable {

            CacheConfiguration cacheConfig = new CacheConfiguration(CACHE_NAME, 100)
                    .terracotta(new TerracottaConfiguration());
            cacheManager.addCache(new Cache(cacheConfig));
            cache = cacheManager.getCache(CACHE_NAME);
            cache.put(new Element("key", "value", true));

            cacheManager.shutdown();

            // Notify client to destroy
            getBarrierForAllClients().await(10, TimeUnit.SECONDS); // hit 1

            // Waiting for other client to signal destroy done
            getBarrierForAllClients().await(1, TimeUnit.MINUTES); // hit 2

            // Making sure adding back cache does not resurrect old data structures
            setupCacheManager();
            cacheManager.addCache(new Cache(cacheConfig));
            cache = cacheManager.getCache(CACHE_NAME);
            assertNull(cache.get("key"));
        }

        @Override
        protected Cache getCache() {
            return null;
        }

    }

    public static class CacheManagerEntityDestroyCrashClient extends ClientBase {

        public CacheManagerEntityDestroyCrashClient(String[] mainArgs) {
            super(mainArgs);
        }

        @Override
        protected void runTest(Cache cache, Toolkit myToolkit) throws Throwable {
            // Waiting for CM to be created

            waitForAllClients(); // hit 1

            Toolkit spiedToolkit = spy(getClusteringToolkit());

            ClusteredEntityManager clusteredEntityManager1 = new ClusteredEntityManager(spiedToolkit);
            Configuration configuration = ConfigurationFactory.parseConfiguration(getEhcacheXmlAsStream());
            String cmName = configuration.getName();

            Map<String, ClusteredCacheManager> cacheManagers = clusteredEntityManager1
                    .getRootEntities(ClusteredCacheManager.class);

            ClusteredCacheManager clusteredCacheManager = cacheManagers.get(cmName);

            while(clusteredCacheManager.isUsed()) {
                TimeUnit.MILLISECONDS.sleep(200);
            }


            doThrow(new TestDestroyCrashException("Crashing destroy"))
                    .when(spiedToolkit)
                    .getCache(any(String.class), any(org.terracotta.toolkit.config.Configuration.class), any(Class.class));
            try {
                clusteredEntityManager1.destroyRootEntity(cmName, ClusteredCacheManager.class, clusteredCacheManager);
                fail("Destroy should have thrown an exception");
            } catch(TestDestroyCrashException e) {
                // Expected as we want destroy to crash
                e.printStackTrace();
            }
            reset(spiedToolkit);

            clusteredEntityManager1.getRootEntity(cmName, ClusteredCacheManager.class);
            // Shows inline clean up performed
            verify(spiedToolkit).getCache(any(String.class), any(org.terracotta.toolkit.config.Configuration.class), any(Class.class));

            reset(spiedToolkit);

            ClusteredEntityManager clusteredEntityManager2 = new ClusteredEntityManager(spiedToolkit);
            assertNull(clusteredEntityManager2.getRootEntity(cmName, ClusteredCacheManager.class));

            verify(spiedToolkit, never()).getCache(any(String.class), any(org.terracotta.toolkit.config.Configuration.class), any(Class.class));

            getBarrierForAllClients().await(10, TimeUnit.SECONDS); // hit 2
        }

        @Override
        protected void setupCacheManager() {
            // Do nothing here
        }

        @Override
        protected Cache getCache() {
            // Do nothing here
            return null;
        }
    }

    public static class TestDestroyCrashException extends RuntimeException {
        public TestDestroyCrashException(String msg) {
            super(msg);
        }
    }
}
