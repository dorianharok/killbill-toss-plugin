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
import org.killbill.billing.plugin.api.notification.PluginTenantConfigurableConfigurationHandler;

/**
 * Configuration handler for the Toss Payments plugin.
 *
 * Manages per-tenant configuration by extending PluginTenantConfigurableConfigurationHandler.
 * Kill Bill's OSGi framework automatically handles TENANT_CONFIG_CHANGE and
 * TENANT_CONFIG_DELETION events to reload configuration.
 */
public class TossConfigurationHandler extends PluginTenantConfigurableConfigurationHandler<TossConfig> {

    public TossConfigurationHandler(final String pluginName, final OSGIKillbillAPI osgiKillbillAPI) {
        super(pluginName, osgiKillbillAPI);
    }

    @Override
    protected TossConfig createConfigurable(final Properties properties) {
        return new TossConfig(properties);
    }
}
