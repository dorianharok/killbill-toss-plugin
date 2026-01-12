package org.killbill.billing.plugin.toss.core;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.killbill.billing.osgi.api.Healthcheck.HealthStatus;
import org.killbill.billing.tenant.api.Tenant;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TossHealthcheckTest {

    private TossConfigurationHandler configHandler;
    private TossHealthcheck healthcheck;

    @BeforeMethod
    public void setUp() {
        configHandler = Mockito.mock(TossConfigurationHandler.class);
        healthcheck = new TossHealthcheck(configHandler);
    }

    @Test
    public void testHealthcheckNoTenant() {
        final HealthStatus status = healthcheck.getHealthStatus(null, null);
        Assert.assertTrue(status.isHealthy());
    }

    @Test
    public void testHealthcheckWithValidConfig() {
        final UUID tenantId = UUID.randomUUID();
        final Tenant tenant = Mockito.mock(Tenant.class);
        Mockito.when(tenant.getId()).thenReturn(tenantId);

        final Properties props = new Properties();
        props.setProperty("org.killbill.billing.plugin.toss.secret_key", "test_sk_xxxxxxxx");
        final TossConfigProperties config = new TossConfigProperties(props);
        Mockito.when(configHandler.getConfigurable(tenantId)).thenReturn(config);

        final HealthStatus status = healthcheck.getHealthStatus(tenant, null);
        Assert.assertTrue(status.isHealthy());
    }

    @Test
    public void testHealthcheckWithNoConfig() {
        final UUID tenantId = UUID.randomUUID();
        final Tenant tenant = Mockito.mock(Tenant.class);
        Mockito.when(tenant.getId()).thenReturn(tenantId);
        Mockito.when(configHandler.getConfigurable(tenantId)).thenReturn(null);

        final HealthStatus status = healthcheck.getHealthStatus(tenant, null);
        Assert.assertFalse(status.isHealthy());
    }

    @Test
    public void testHealthcheckWithEmptySecretKey() {
        final UUID tenantId = UUID.randomUUID();
        final Tenant tenant = Mockito.mock(Tenant.class);
        Mockito.when(tenant.getId()).thenReturn(tenantId);

        final Properties props = new Properties();
        final TossConfigProperties config = new TossConfigProperties(props);
        Mockito.when(configHandler.getConfigurable(tenantId)).thenReturn(config);

        final HealthStatus status = healthcheck.getHealthStatus(tenant, null);
        Assert.assertFalse(status.isHealthy());
    }

    @Test
    public void testHealthcheckWithInvalidSecretKeyFormat() {
        final UUID tenantId = UUID.randomUUID();
        final Tenant tenant = Mockito.mock(Tenant.class);
        Mockito.when(tenant.getId()).thenReturn(tenantId);

        final Properties props = new Properties();
        props.setProperty("org.killbill.billing.plugin.toss.secret_key", "invalid_key_format");
        final TossConfigProperties config = new TossConfigProperties(props);
        Mockito.when(configHandler.getConfigurable(tenantId)).thenReturn(config);

        final HealthStatus status = healthcheck.getHealthStatus(tenant, null);
        Assert.assertFalse(status.isHealthy());
    }

    @Test
    public void testHealthcheckWithLiveSecretKey() {
        final UUID tenantId = UUID.randomUUID();
        final Tenant tenant = Mockito.mock(Tenant.class);
        Mockito.when(tenant.getId()).thenReturn(tenantId);

        final Properties props = new Properties();
        props.setProperty("org.killbill.billing.plugin.toss.secret_key", "live_sk_xxxxxxxx");
        final TossConfigProperties config = new TossConfigProperties(props);
        Mockito.when(configHandler.getConfigurable(tenantId)).thenReturn(config);

        final HealthStatus status = healthcheck.getHealthStatus(tenant, null);
        Assert.assertTrue(status.isHealthy());
    }
}
