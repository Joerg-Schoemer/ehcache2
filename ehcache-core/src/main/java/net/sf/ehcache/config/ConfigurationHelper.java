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

package net.sf.ehcache.config;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration.CacheDecoratorFactoryConfiguration;
import net.sf.ehcache.constructs.CacheDecoratorFactory;
import net.sf.ehcache.distribution.CacheManagerPeerListener;
import net.sf.ehcache.distribution.CacheManagerPeerListenerFactory;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CacheManagerPeerProviderFactory;
import net.sf.ehcache.event.CacheManagerEventListener;
import net.sf.ehcache.event.CacheManagerEventListenerFactory;
import net.sf.ehcache.exceptionhandler.CacheExceptionHandler;
import net.sf.ehcache.exceptionhandler.CacheExceptionHandlerFactory;
import net.sf.ehcache.exceptionhandler.ExceptionHandlingDynamicCacheProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static net.sf.ehcache.util.ClassLoaderUtil.createNewInstance;
import static net.sf.ehcache.util.PropertyUtil.parseProperties;

/**
 * The configuration for ehcache.
 * <p>
 * This class can be populated through:
 * <ul>
 * <li>introspection by {@link ConfigurationFactory} or
 * <li>programmatically
 * </ul>
 *
 * @author Greg Luck
 * @version $Id$
 */
public final class ConfigurationHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationHelper.class.getName());

    private final Configuration configuration;
    private final CacheManager cacheManager;

    private final ClassLoader loader;

    public ConfigurationHelper(CacheManager cacheManager, Configuration configuration) {
        if (cacheManager == null || configuration == null) {
            throw new IllegalArgumentException("Cannot have null parameters");
        }
        this.cacheManager = cacheManager;
        this.configuration = configuration;
        this.loader = configuration.getClassLoader();
    }

    /**
     * Tries to create a CacheLoader from the configuration using the factory
     * specified.
     *
     * @return The CacheExceptionHandler, or null if it could not be found.
     */
    public static CacheExceptionHandler createCacheExceptionHandler(
            CacheConfiguration.CacheExceptionHandlerFactoryConfiguration factoryConfiguration,
            ClassLoader loader
    ) throws CacheException {
        String className = null;
        if (factoryConfiguration != null) {
            className = factoryConfiguration.getFullyQualifiedClassPath();
        }
        if (className == null || className.length() == 0) {
            LOG.debug("No CacheExceptionHandlerFactory class specified. Skipping...");
            return null;
        }

        CacheExceptionHandlerFactory factory = (CacheExceptionHandlerFactory) createNewInstance(loader, className);
        Properties properties = parseProperties(factoryConfiguration.getProperties(), factoryConfiguration.getPropertySeparator());

        return factory.createExceptionHandler(properties);
    }

    /**
     * Tries to load the class specified otherwise defaults to null
     *
     * @return a map of CacheManagerPeerProviders
     */
    public Map<String, CacheManagerPeerProvider> createCachePeerProviders() {
        Map<String, CacheManagerPeerProvider> cacheManagerPeerProviders = new HashMap<>();
        for (FactoryConfiguration<?> factoryConfiguration : configuration.getCacheManagerPeerProviderFactoryConfiguration()) {
            String className = null;
            if (factoryConfiguration != null) {
                className = factoryConfiguration.getFullyQualifiedClassPath();
            }
            if (className == null) {
                LOG.debug("No CachePeerProviderFactoryConfiguration specified. Not configuring a CacheManagerPeerProvider.");
                continue;
            }
            CacheManagerPeerProviderFactory cacheManagerPeerProviderFactory = (CacheManagerPeerProviderFactory) createNewInstance(loader, className);
            Properties properties = parseProperties(factoryConfiguration.getProperties(), factoryConfiguration.getPropertySeparator());
            CacheManagerPeerProvider cacheManagerPeerProvider = cacheManagerPeerProviderFactory.createCachePeerProvider(cacheManager, properties);
            cacheManagerPeerProviders.put(cacheManagerPeerProvider.getScheme(), cacheManagerPeerProvider);
        }

        return cacheManagerPeerProviders;
    }

    /**
     * Tries to load the class specified otherwise defaults to null
     */
    public Map<String, CacheManagerPeerListener> createCachePeerListeners() {
        Map<String, CacheManagerPeerListener> cacheManagerPeerListeners = new HashMap<>();
        for (FactoryConfiguration<?> factoryConfiguration : configuration.getCacheManagerPeerListenerFactoryConfigurations()) {

            String className = null;
            if (factoryConfiguration != null) {
                className = factoryConfiguration.getFullyQualifiedClassPath();
            }
            if (className == null) {
                LOG.debug("No CachePeerListenerFactoryConfiguration specified. Not configuring a CacheManagerPeerListener.");
                continue;
            }

            CacheManagerPeerListenerFactory listenerFactory = (CacheManagerPeerListenerFactory) createNewInstance(loader, className);
            Properties properties = parseProperties(factoryConfiguration.getProperties(), factoryConfiguration.getPropertySeparator());
            CacheManagerPeerListener peerListener = listenerFactory.createCachePeerListener(cacheManager, properties);
            cacheManagerPeerListeners.put(peerListener.getScheme(), peerListener);
        }

        return cacheManagerPeerListeners;
    }

    /**
     * Tries to load the class specified.
     *
     * @return If there is none returns null.
     */
    public CacheManagerEventListener createCacheManagerEventListener(CacheManager cacheManager) throws CacheException {
        FactoryConfiguration<?> cacheManagerEventListenerFactoryConfiguration =
                configuration.getCacheManagerEventListenerFactoryConfiguration();

        String className = null;
        if (cacheManagerEventListenerFactoryConfiguration != null) {
            className = cacheManagerEventListenerFactoryConfiguration.getFullyQualifiedClassPath();
        }
        if (className == null || className.length() == 0) {
            LOG.debug("No CacheManagerEventListenerFactory class specified. Skipping...");

            return null;
        }

        CacheManagerEventListenerFactory factory = (CacheManagerEventListenerFactory) createNewInstance(loader, className);
        Properties properties = parseProperties(cacheManagerEventListenerFactoryConfiguration.properties, cacheManagerEventListenerFactoryConfiguration.getPropertySeparator());

        return factory.createCacheManagerEventListener(cacheManager, properties);
    }


    /**
     * @return the disk store path, or null if not set.
     */
    public String getDiskStorePath() {
        DiskStoreConfiguration diskStoreConfiguration = configuration.getDiskStoreConfiguration();
        if (diskStoreConfiguration == null) {
            return null;
        } else {
            return diskStoreConfiguration.getPath();
        }
    }

    /**
     * @return the Default Cache
     * @throws net.sf.ehcache.CacheException if there is no default cache
     */
    public Ehcache createDefaultCache() throws CacheException {
        CacheConfiguration cacheConfiguration = configuration.getDefaultCacheConfiguration();
        if (cacheConfiguration == null) {
            return null;
        } else {
            cacheConfiguration.name = Cache.DEFAULT_CACHE_NAME;
            return createCache(cacheConfiguration);
        }
    }

    /**
     * Creates unitialised caches for each cache configuration found
     *
     * @return an empty set if there are none,
     */
    public Set<Ehcache> createCaches() {
        return configuration.getCacheConfigurations()
                .values()
                .stream()
                .map(this::createCache)
                .collect(Collectors.toSet());
    }

    /**
     * Calculates the number of caches in the configuration that are diskPersistent
     */
    public int numberOfCachesThatUseDiskStorage() {
        int count = 0;
        for (CacheConfiguration cacheConfig : configuration.getCacheConfigurations().values()) {
            if (cacheConfig.isOverflowToDisk()
                    || cacheConfig.isDiskPersistent()
                    || cacheConfig.isOverflowToOffHeap() && cacheConfig.isSearchable()
            ) {
                count++;
                continue;
            }

            PersistenceConfiguration persistence = cacheConfig.getPersistenceConfiguration();
            if (persistence == null) {
                continue;
            }

            switch (persistence.getStrategy()) {
                case LOCALTEMPSWAP:
                case LOCALRESTARTABLE:
                    count++;
                    break;
                default:
            }
        }

        return count;
    }

    /**
     * Creates a cache from configuration where the configuration cache name matches the given name
     *
     * @return the cache, or null if there is no match
     */
    Ehcache createCacheFromName(String name) {

        return configuration.getCacheConfigurations()
                .values()
                .stream()
                .filter(entry -> entry.name.equals(name))
                .findFirst()
                .map(this::createCache)
                .orElse(null);
    }

    /**
     * Create a cache given a cache configuration.
     */
    Ehcache createCache(CacheConfiguration cacheConfiguration) {
        CacheConfiguration configClone = cacheConfiguration.clone();

        // make sure all caches use the same classloader that the CacheManager is configured to use
        configClone.setClassLoader(configuration.getClassLoader());

        Ehcache cache = new Cache(configClone, null, null);

        return applyCacheExceptionHandler(configClone, cache);
    }

    private Ehcache applyCacheExceptionHandler(CacheConfiguration cacheConfiguration, Ehcache cache) {
        CacheExceptionHandler cacheExceptionHandler = createCacheExceptionHandler(cacheConfiguration.getCacheExceptionHandlerFactoryConfiguration(), loader);

        cache.setCacheExceptionHandler(cacheExceptionHandler);

        if (cache.getCacheExceptionHandler() != null) {
            return ExceptionHandlingDynamicCacheProxy.createProxy(cache);
        }

        return cache;
    }

    /**
     * Creates decorated ehcaches for the cache, if any configured in ehcache.xml
     *
     * @param cache the cache
     * @return List of the decorated ehcaches, if any configured in ehcache.xml otherwise returns empty list
     */
    public List<Ehcache> createCacheDecorators(Ehcache cache) {
        CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
        if (cacheConfiguration == null) {
            return createDefaultCacheDecorators(cache, configuration.getDefaultCacheConfiguration(), loader);
        }
        List<CacheDecoratorFactoryConfiguration> cacheDecoratorConfigurations = cacheConfiguration.getCacheDecoratorConfigurations();
        if (cacheDecoratorConfigurations == null || cacheDecoratorConfigurations.size() == 0) {
            LOG.debug("CacheDecoratorFactory not configured. Skipping for '" + cache.getName() + "'.");
            return createDefaultCacheDecorators(cache, configuration.getDefaultCacheConfiguration(), loader);
        }

        List<Ehcache> result = cacheDecoratorConfigurations
                .stream()
                .map(factoryConfiguration -> createDecoratedCache(cache, factoryConfiguration, false, loader))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        result.addAll(createDefaultCacheDecorators(cache, configuration.getDefaultCacheConfiguration(), loader));

        return result;
    }

    /**
     * Creates default cache decorators specified in the default cache configuration if any
     *
     * @param cache                     the underlying cache that will be decorated
     * @param defaultCacheConfiguration default cache configuration
     * @return list of decorated caches
     */
    public static List<Ehcache> createDefaultCacheDecorators(Ehcache cache, CacheConfiguration defaultCacheConfiguration, ClassLoader loader) {
        if (cache == null) {
            throw new CacheException("Underlying cache cannot be null when creating decorated caches.");
        }
        List<CacheDecoratorFactoryConfiguration> defaultCacheDecoratorConfigurations = defaultCacheConfiguration == null ?
                null : defaultCacheConfiguration.getCacheDecoratorConfigurations();
        if (defaultCacheDecoratorConfigurations == null || defaultCacheDecoratorConfigurations.size() == 0) {
            LOG.debug("CacheDecoratorFactory not configured for defaultCache. Skipping for '" + cache.getName() + "'.");
            return Collections.emptyList();
        }
        List<Ehcache> result = new ArrayList<>();
        Set<String> newCacheNames = new HashSet<>();
        for (CacheDecoratorFactoryConfiguration factoryConfiguration : defaultCacheDecoratorConfigurations) {
            Ehcache decoratedCache = createDecoratedCache(cache, factoryConfiguration, true, loader);
            if (decoratedCache == null) {
                continue;
            }
            if (newCacheNames.contains(decoratedCache.getName())) {
                throw new InvalidConfigurationException(
                        "Looks like the defaultCache is configured with multiple CacheDecoratorFactory's "
                                + "that does not set unique names for newly created caches. Please fix the "
                                + "CacheDecoratorFactory and/or the config to set unique names for newly created caches.");
            }
            newCacheNames.add(decoratedCache.getName());
            result.add(decoratedCache);
        }

        return result;
    }

    /**
     * Creates the decorated cache from the decorator config specified. Returns null if the name of the factory class is not specified
     */
    private static Ehcache createDecoratedCache(
            Ehcache cache,
            CacheConfiguration.CacheDecoratorFactoryConfiguration factoryConfiguration,
            boolean forDefaultCache,
            ClassLoader loader
    ) {
        if (factoryConfiguration == null) {
            return null;
        }
        String className = factoryConfiguration.getFullyQualifiedClassPath();
        if (className == null) {
            LOG.debug("CacheDecoratorFactory was specified without the name of the factory. Skipping...");
            return null;
        }

        CacheDecoratorFactory factory = (CacheDecoratorFactory) createNewInstance(loader, className);
        Properties properties = parseProperties(factoryConfiguration.getProperties(), factoryConfiguration.getPropertySeparator());
        if (forDefaultCache) {
            return factory.createDefaultDecoratedEhcache(cache, properties);
        }

        return factory.createDecoratedEhcache(cache, properties);
    }

    /**
     * @param sa search attribute
     * @return attribute type as class
     * @deprecated internal use only
     */
    public static Class<?> getSearchAttributeType(SearchAttribute sa, ClassLoader loader) {
        return sa.getType(loader);
    }

    /**
     * @return the Configuration used
     */
    public Configuration getConfigurationBean() {
        return configuration;
    }
}
