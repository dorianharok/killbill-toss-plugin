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

import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TossConfigurationHandlerTest {

    private static final String PLUGIN_NAME = "killbill-toss";
    private static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.toss.";

    private OSGIKillbillAPI killbillAPI;
    private TossConfigurationHandler handler;

    @BeforeMethod(groups = "fast")
    public void setUp() {
        killbillAPI = Mockito.mock(OSGIKillbillAPI.class);
        handler = new TossConfigurationHandler(PLUGIN_NAME, killbillAPI);
    }

    @Test(groups = "fast")
    public void testCreateConfigurable() {
        final Properties properties = new Properties();
        properties.setProperty(PROPERTY_PREFIX + "secret_key", "test_sk_handler123");
        properties.setProperty(PROPERTY_PREFIX + "connection_timeout", "7000");

        final TossConfigProperties config = handler.createConfigurable(properties);

        Assert.assertNotNull(config);
        Assert.assertEquals(config.getSecretKey(), "test_sk_handler123");
        Assert.assertEquals(config.getConnectionTimeout(), 7000);
    }

    @Test(groups = "fast")
    public void testCreateConfigurableWithEmptyProperties() {
        final Properties properties = new Properties();

        final TossConfigProperties config = handler.createConfigurable(properties);

        Assert.assertNotNull(config);
        Assert.assertNull(config.getSecretKey());
        Assert.assertEquals(config.getConnectionTimeout(), TossConfigProperties.DEFAULT_CONNECTION_TIMEOUT);
        Assert.assertEquals(config.getReadTimeout(), TossConfigProperties.DEFAULT_READ_TIMEOUT);
    }

    @Test(groups = "fast")
    public void testHandlerInheritsFromPluginTenantConfigurableConfigurationHandler() {
        Assert.assertTrue(handler instanceof org.killbill.billing.plugin.api.notification.PluginTenantConfigurableConfigurationHandler);
    }
}
