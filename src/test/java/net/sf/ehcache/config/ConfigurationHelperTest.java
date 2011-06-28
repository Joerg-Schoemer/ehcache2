/**
 *  Copyright 2003-2010 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.config;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheManager;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests programmatically constructed Configuration instances
 *
 * @author Greg Luck
 * @version $Id$
 */
public class ConfigurationHelperTest extends AbstractCacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationHelperTest.class.getName());

    @Test
    public void testMaxBytesOnConfiguration() {
        Configuration configuration = new Configuration()
            .maxOnHeap(20, MemoryUnit.MEGABYTES)
            .maxOffHeap(2, MemoryUnit.GIGABYTES)
            .maxOnDisk(2000, MemoryUnit.GIGABYTES);

        assertThat(configuration.getMaxBytesOnHeap(), is(MemoryUnit.MEGABYTES.toBytes(20)));
        assertThat(configuration.getMaxBytesOffHeap(), is((long) 2 * 1024 * 1024 * 1024));
        assertThat(configuration.getMaxBytesOnDisk(), is((long) 2000 * 1024 * 1024 * 1024));

        configuration.setMaxBytesOnHeap((Long) null);
        assertThat(configuration.getMaxBytesOnHeap(), is(0L));
        assertThat(configuration.getMaxBytesOffHeap(), is((long) 2 * 1024 * 1024 * 1024));
        assertThat(configuration.getMaxBytesOnDisk(), is((long)2000 * 1024 * 1024 * 1024));

        configuration.setMaxBytesOffHeap((Long)null);
        assertThat(configuration.getMaxBytesOnHeap(), is(0L));
        assertThat(configuration.getMaxBytesOffHeap(), is(0L));
        assertThat(configuration.getMaxBytesOnDisk(), is((long)2000 * 1024 * 1024 * 1024));

        configuration.setMaxBytesOnDisk((Long)null);
        assertThat(configuration.getMaxBytesOnHeap(), is(0L));
        assertThat(configuration.getMaxBytesOffHeap(), is(0L));
        assertThat(configuration.getMaxBytesOnDisk(), is(0L));

        configuration = new Configuration();
        configuration.setMaxBytesOnHeap("1g");
        configuration.setMaxBytesOffHeap("12G");
        configuration.setMaxBytesOnDisk("200G");

        assertThat(configuration.getMaxBytesOnHeap(), is(MemoryUnit.GIGABYTES.toBytes(1)));
        assertThat(configuration.getMaxBytesOffHeap(), is(MemoryUnit.GIGABYTES.toBytes(12)));
        assertThat(configuration.getMaxBytesOnDisk(), is(MemoryUnit.GIGABYTES.toBytes(200)));
    }

    /**
     * Should not give exceptions
     */
    @Test
    public void testValidParameters() {
        Configuration configuration = new Configuration();
        CacheConfiguration defaultCache = new CacheConfiguration()
                .eternal(false);

        ConfigurationHelper configurationHelper =
                new ConfigurationHelper(manager, configuration);
        assertNotNull(configurationHelper);
    }

    /**
     * Will fail if all params null
     */
    @Test
    public void testNullParameters() {
        try {
            new ConfigurationHelper((CacheManager) null, null);
            fail();
        } catch (Exception e) {
            //expected
            LOG.debug("Expected exception " + e.getMessage() + ". Initial cause was " + e.getMessage(), e);
        }
    }

    /**
     * Test the expansion of Java system properties.
     * These can be mixed in with other path information, in which case they should be expanded and the other
     * path information catenatated.
     *
     * @throws IOException
     */
    @Test
    public void testDiskStorePathExpansion() throws IOException {
        DiskStoreConfiguration diskStore = new DiskStoreConfiguration();

        specificPathTest(diskStore, "java.io.tmpdir", "java.io.tmpdir");
        specificPathTest(diskStore, "java.io.tmpdir/cacheManager1", "java.io.tmpdir");
        specificPathTest(diskStore, "java.io.tmpdir/cacheManager1/", "java.io.tmpdir");
        specificPathTest(diskStore, "user.dir", "user.dir");
        specificPathTest(diskStore, "user.dir/cacheManager1", "user.dir");
        specificPathTest(diskStore, "user.dir/cacheManager1/", "user.dir");
        specificPathTest(diskStore, "user.home", "user.home");
        specificPathTest(diskStore, "user.home/cacheManager1", "user.home");
        specificPathTest(diskStore, "user.home/cacheManager1/", "user.home");
        specificPathTest(diskStore, "user.home/cacheManager1/dir1", "user.home");

        specificPathTest(diskStore, "${java.io.tmpdir}", "java.io.tmpdir");
        specificPathTest(diskStore, "${java.io.tmpdir}/cacheManager1", "java.io.tmpdir");
        specificPathTest(diskStore, "${java.io.tmpdir}/cacheManager1/", "java.io.tmpdir");
        specificPathTest(diskStore, "${user.dir}", "user.dir");
        specificPathTest(diskStore, "${user.dir}/cacheManager1", "user.dir");
        specificPathTest(diskStore, "${user.dir}/cacheManager1/", "user.dir");
        specificPathTest(diskStore, "${user.home}", "user.home");
        specificPathTest(diskStore, "${user.home}/cacheManager1", "user.home");
        specificPathTest(diskStore, "${user.home}/cacheManager1/", "user.home");
        specificPathTest(diskStore, "${user.home}/cacheManager1/dir1", "user.home");

        System.setProperty("my-special-property", "hello");
        specificPathTest(diskStore, "${user.home}/cacheManager1/${my-special-property}/world", "user.home", "my-special-property");
        specificPathTest(diskStore, "user.home/cacheManager1/${my-special-property}/world", "user.home", "my-special-property");

        System.setProperty("ehcache.disk.store.dir", "/tmp");
        specificPathTest(diskStore, "ehcache.disk.store.dir/cacheManager1/dir1", "ehcache.disk.store.dir");
        specificPathTest(diskStore, "${ehcache.disk.store.dir}/cacheManager1/dir1", "ehcache.disk.store.dir");
    }

    private void specificPathTest(DiskStoreConfiguration diskStoreConfiguration, String specifiedPath, String ... properties) {
        diskStoreConfiguration.setPath(specifiedPath);
        String expandedPath = diskStoreConfiguration.getPath();
        for (String prop : properties) {
            assertFalse(expandedPath.contains(prop));
            assertTrue(expandedPath.contains(System.getProperty(prop)));
        }

        File diskDir = null;
        try {
            diskDir = new File(expandedPath);
            diskDir.mkdirs();
            assertTrue(diskDir.exists());
            assertTrue(diskDir.isDirectory());
        } finally {
            //delete only paths we created, not existing system paths, for repeatability
            while (diskDir.getPath().indexOf("cacheManager1") != -1) {
                diskDir.delete();
                diskDir = diskDir.getParentFile();
            }
        }
    }
}
