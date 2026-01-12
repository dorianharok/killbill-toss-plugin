package org.killbill.billing.plugin.toss.core;

import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.tenant.api.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TossHealthcheck implements Healthcheck {

    private static final Logger logger = LoggerFactory.getLogger(TossHealthcheck.class);

    private final TossConfigurationHandler configurationHandler;

    public TossHealthcheck(final TossConfigurationHandler configurationHandler) {
        this.configurationHandler = configurationHandler;
    }

    @Override
    public HealthStatus getHealthStatus(@Nullable final Tenant tenant, @Nullable final Map properties) {
        if (tenant == null) {
            return HealthStatus.healthy("Toss plugin running");
        }
        return checkTenantConfiguration(tenant.getId());
    }

    private HealthStatus checkTenantConfiguration(final UUID tenantId) {
        try {
            final TossConfigProperties config = configurationHandler.getConfigurable(tenantId);
            
            if (config == null) {
                logger.warn("Healthcheck failed: No configuration found for tenant {}", tenantId);
                return HealthStatus.unHealthy("Toss configuration not found for tenant");
            }

            final String secretKey = config.getSecretKey();
            if (secretKey == null || secretKey.isEmpty()) {
                logger.warn("Healthcheck failed: Secret key not configured for tenant {}", tenantId);
                return HealthStatus.unHealthy("Toss secretKey not configured");
            }

            if (!isValidSecretKeyFormat(secretKey)) {
                logger.warn("Healthcheck failed: Invalid secret key format for tenant {}", tenantId);
                return HealthStatus.unHealthy("Invalid Toss secretKey format");
            }

            logger.debug("Healthcheck passed for tenant {}", tenantId);
            return HealthStatus.healthy("Toss OK");

        } catch (final Exception e) {
            logger.error("Healthcheck error for tenant {}", tenantId, e);
            return HealthStatus.unHealthy("Toss error: " + e.getMessage());
        }
    }

    private boolean isValidSecretKeyFormat(final String secretKey) {
        return secretKey.startsWith("test_sk") || 
               secretKey.startsWith("live_sk") ||
               secretKey.startsWith("test_gsk") ||
               secretKey.startsWith("live_gsk");
    }
}
