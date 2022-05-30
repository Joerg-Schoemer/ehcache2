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

package net.sf.ehcache.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

/**
 * Property utilities.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class PropertyUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyUtil.class);

    private static final String DEFAULT_PROPERTY_SEPARATOR = ",";

    /**
     * Utility class therefore no constructor.
     */
    private PropertyUtil() {
        //noop
    }

    /**
     * @return defaultValue if there is no property for the key, or there are no properties
     */
    public static String extractString(Properties properties, String propertyName, String defaultValue) {
        if (properties == null || properties.size() == 0) {
            return defaultValue;
        }

        String value = (String) properties.get(propertyName);
        if (value == null) {
            LOG.debug("Value not found for {}: taking default {}", propertyName, defaultValue);
            return defaultValue;
        }

        value = value.trim();
        LOG.debug("Value found for {}: {}", propertyName, value);
        return value;

    }

    public static String extractString(Properties properties, String propertyName) {
        return extractString(properties, propertyName, null);
    }

    /**
     * Parse properties supplied as a comma separated list into a <code>Properties</code> object
     *
     * @param propertiesString a comma separated list such as <code>"propertyA=s, propertyB=t"</code>
     * @return a newly constructed properties object
     */
    public static Properties parseProperties(String propertiesString, String propertySeparator) {
        if (propertiesString == null) {
            LOG.debug("propertiesString is null.");
            return null;
        }
        if (propertySeparator == null) {
            propertySeparator = DEFAULT_PROPERTY_SEPARATOR;
        }
        Properties properties = new Properties();
        String propertyLines = propertiesString.trim();
        propertyLines = propertyLines.replaceAll(propertySeparator, "\n");
        try {
            properties.load(new StringReader(propertyLines));
        } catch (IOException e) {
            LOG.error("Cannot load properties from " + propertiesString);
        }
        return properties;
    }

    /**
     * Null safe, parser of boolean from a String
     *
     * @return true if not null and case insensitively matches true
     */
    public static boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value);
    }

    /**
     * Will retrieve the boolean value from the properties, defaulting if property isn't present
     *
     * @param properties   the properties to use
     * @param propertyName the property name to look for
     * @param defaultValue the default value if property is missing
     * @return the value, or it's default, for the property
     */
    public static boolean extractBoolean(Properties properties, String propertyName, boolean defaultValue) {
        String value = extractString(properties, propertyName, Boolean.toString(defaultValue));

        return parseBoolean(value);
    }

    /**
     * Will retrieve the long value from the properties, defaulting if property isn't present
     *
     * @param properties   the properties to use
     * @param propertyName the property name to look for
     * @param defaultValue the default value if property is missing
     * @return the value, or it's default, for the property
     */
    public static long extractLong(Properties properties, String propertyName, long defaultValue) {
        String value = extractString(properties, propertyName, Long.toString(defaultValue));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            LOG.warn("could not parse value {} to long", value);
            return defaultValue;
        }
    }

    /**
     * Will retrieve the integer value from the properties, defaulting if property isn't present
     *
     * @param properties   the properties to use
     * @param propertyName the property name to look for
     * @param defaultValue the default value if property is missing
     * @return the value, or it's default, for the property
     */
    public static int extractInt(Properties properties, String propertyName, int defaultValue) {
        String value = extractString(properties, propertyName, Integer.toString(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOG.warn("could not parse value {} to int", value);
            return defaultValue;
        }
    }
}
