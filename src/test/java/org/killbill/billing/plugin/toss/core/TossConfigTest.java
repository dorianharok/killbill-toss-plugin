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

import org.testng.Assert;
import org.testng.annotations.Test;

public class TossConfigTest {

    private static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.toss.";

    @Test(groups = "fast")
    public void testConfigWithAllProperties() {
        final Properties properties = new Properties();
        properties.setProperty(PROPERTY_PREFIX + "secret_key", "test_sk_abc123");
        properties.setProperty(PROPERTY_PREFIX + "connection_timeout", "3000");
        properties.setProperty(PROPERTY_PREFIX + "read_timeout", "4000");
        properties.setProperty(PROPERTY_PREFIX + "test_mode", "true");

        final TossConfig config = new TossConfig(properties);

        Assert.assertEquals(config.getSecretKey(), "test_sk_abc123");
        Assert.assertEquals(config.getConnectionTimeout(), 3000);
        Assert.assertEquals(config.getReadTimeout(), 4000);
        Assert.assertTrue(config.isTestMode());
    }

    @Test(groups = "fast")
    public void testConfigWithDefaultValues() {
        final Properties properties = new Properties();

        final TossConfig config = new TossConfig(properties);

        Assert.assertNull(config.getSecretKey());
        Assert.assertEquals(config.getConnectionTimeout(), TossConfig.DEFAULT_CONNECTION_TIMEOUT);
        Assert.assertEquals(config.getReadTimeout(), TossConfig.DEFAULT_READ_TIMEOUT);
        Assert.assertFalse(config.isTestMode());
    }

    @Test(groups = "fast")
    public void testConfigDefaultTimeoutValues() {
        Assert.assertEquals(TossConfig.DEFAULT_CONNECTION_TIMEOUT, 5000);
        Assert.assertEquals(TossConfig.DEFAULT_READ_TIMEOUT, 5000);
    }

    @Test(groups = "fast")
    public void testToStringMasksSecretKey() {
        final Properties properties = new Properties();
        properties.setProperty(PROPERTY_PREFIX + "secret_key", "test_sk_very_secret_key_12345");

        final TossConfig config = new TossConfig(properties);
        final String toString = config.toString();

        Assert.assertFalse(toString.contains("test_sk_very_secret_key_12345"),
                "Secret key should not appear in toString()");
        Assert.assertTrue(toString.contains("****2345"),
                "Secret key should be masked showing only last 4 characters");
        Assert.assertFalse(toString.contains("test_sk"),
                "Secret key prefix should not be visible");
    }

    @Test(groups = "fast")
    public void testToStringWithNullSecretKey() {
        final Properties properties = new Properties();

        final TossConfig config = new TossConfig(properties);
        final String toString = config.toString();

        Assert.assertNotNull(toString);
        Assert.assertFalse(toString.contains("null"));
    }

    @Test(groups = "fast")
    public void testPartialConfiguration() {
        final Properties properties = new Properties();
        properties.setProperty(PROPERTY_PREFIX + "secret_key", "test_sk_partial");
        // connection_timeout and read_timeout not set

        final TossConfig config = new TossConfig(properties);

        Assert.assertEquals(config.getSecretKey(), "test_sk_partial");
        Assert.assertEquals(config.getConnectionTimeout(), TossConfig.DEFAULT_CONNECTION_TIMEOUT);
        Assert.assertEquals(config.getReadTimeout(), TossConfig.DEFAULT_READ_TIMEOUT);
        Assert.assertFalse(config.isTestMode());
    }

    @Test(groups = "fast")
    public void testTestModeVariations() {
        // Test "true" string
        Properties properties = new Properties();
        properties.setProperty(PROPERTY_PREFIX + "test_mode", "true");
        Assert.assertTrue(new TossConfig(properties).isTestMode());

        // Test "false" string
        properties = new Properties();
        properties.setProperty(PROPERTY_PREFIX + "test_mode", "false");
        Assert.assertFalse(new TossConfig(properties).isTestMode());

        // Test invalid value defaults to false
        properties = new Properties();
        properties.setProperty(PROPERTY_PREFIX + "test_mode", "invalid");
        Assert.assertFalse(new TossConfig(properties).isTestMode());
    }

    @Test(groups = "fast")
    public void testInvalidTimeoutValuesFallbackToDefault() {
        final Properties properties = new Properties();
        // Invalid format (with unit "ms")
        properties.setProperty(PROPERTY_PREFIX + "connection_timeout", "5000ms");
        properties.setProperty(PROPERTY_PREFIX + "read_timeout", "invalid");

        final TossConfig config = new TossConfig(properties);

        // Should fall back to defaults and log warning
        Assert.assertEquals(config.getConnectionTimeout(), TossConfig.DEFAULT_CONNECTION_TIMEOUT);
        Assert.assertEquals(config.getReadTimeout(), TossConfig.DEFAULT_READ_TIMEOUT);
    }

    @Test(groups = "fast")
    public void testShortSecretKeyMasking() {
        final Properties properties = new Properties();
        // 8 characters or less should be fully masked
        properties.setProperty(PROPERTY_PREFIX + "secret_key", "short123");

        final TossConfig config = new TossConfig(properties);
        final String toString = config.toString();

        Assert.assertTrue(toString.contains("[MASKED]"),
                "Short secret keys should be fully masked");
        Assert.assertFalse(toString.contains("short123"),
                "Short secret key should not be visible");
    }
}
