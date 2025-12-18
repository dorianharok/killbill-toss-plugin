/*
 * Copyright 2025 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.toss.core;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration properties for the Toss Payments plugin.
 *
 * Reads configuration from killbill.properties or per-tenant configuration.
 * Secret keys are masked in toString() to prevent accidental logging.
 */
public class TossConfigProperties {

    private static final Logger logger = LoggerFactory.getLogger(TossConfigProperties.class);
    private static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.toss.";

    /** Default connection timeout in milliseconds */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5000;

    /** Default read timeout in milliseconds */
    public static final int DEFAULT_READ_TIMEOUT = 5000;

    private final String secretKey;
    private final int connectionTimeout;
    private final int readTimeout;
    private final boolean testMode;

    /**
     * Creates a TossConfigProperties from the given properties.
     *
     * @param properties Configuration properties with keys prefixed by "org.killbill.billing.plugin.toss."
     */
    public TossConfigProperties(final Properties properties) {
        this.secretKey = properties.getProperty(PROPERTY_PREFIX + "secret_key");
        this.connectionTimeout = parseIntProperty(properties, "connection_timeout", DEFAULT_CONNECTION_TIMEOUT);
        this.readTimeout = parseIntProperty(properties, "read_timeout", DEFAULT_READ_TIMEOUT);
        this.testMode = Boolean.parseBoolean(properties.getProperty(PROPERTY_PREFIX + "test_mode", "false"));

        if (secretKey == null || secretKey.trim().isEmpty()) {
            logger.warn("Toss Payments secret key is not configured. " +
                    "Set 'org.killbill.billing.plugin.toss.secret_key' in killbill.properties or per-tenant configuration.");
        }
    }

    private int parseIntProperty(final Properties properties, final String key, final int defaultValue) {
        final String value = properties.getProperty(PROPERTY_PREFIX + key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            logger.warn("Invalid integer value for property '{}': '{}'. Using default: {}",
                    PROPERTY_PREFIX + key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * @return The Toss Payments secret key for API authentication
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * @return HTTP connection timeout in milliseconds
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * @return HTTP read timeout in milliseconds
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * @return true if test/sandbox mode is enabled
     */
    public boolean isTestMode() {
        return testMode;
    }

    @Override
    public String toString() {
        return "TossConfigProperties{" +
                "secretKey=" + maskSecretKey(secretKey) +
                ", connectionTimeout=" + connectionTimeout +
                ", readTimeout=" + readTimeout +
                ", testMode=" + testMode +
                '}';
    }

    private String maskSecretKey(final String key) {
        if (key == null || key.isEmpty()) {
            return "[NOT SET]";
        }
        // Only show last 4 characters to minimize exposure
        if (key.length() <= 8) {
            return "[MASKED]";
        }
        return "****" + key.substring(key.length() - 4);
    }
}
