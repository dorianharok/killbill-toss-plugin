package org.killbill.billing.plugin.toss.core;

import java.util.Hashtable;

import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.plugin.toss.TossPaymentPluginApi;
import org.killbill.billing.plugin.toss.client.TossClient;
import org.killbill.billing.plugin.toss.client.TossClientImpl;
import org.killbill.billing.plugin.toss.dao.TossDao;
import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TossActivator extends KillbillActivatorBase {

    private static final Logger logger = LoggerFactory.getLogger(TossActivator.class);

    public static final String PLUGIN_NAME = "killbill-toss";

    private TossConfigurationHandler configurationHandler;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        logger.info("TossPluginActivator starting");

        // Initialize configuration handler
        configurationHandler = new TossConfigurationHandler(PLUGIN_NAME, killbillAPI);

        // Create configuration object explicitly and set as default
        final TossConfigProperties globalConfiguration =
            configurationHandler.createConfigurable(configProperties.getProperties());
        configurationHandler.setDefaultConfigurable(globalConfiguration);

        final TossDao dao = new TossDao(dataSource.getDataSource());
        final TossClient tossClient = new TossClientImpl();
        final TossPaymentPluginApi api = new TossPaymentPluginApi(killbillAPI, configProperties, clock.getClock(), dao, configurationHandler, tossClient);
        registerPaymentPluginApi(context, api);

        logger.info("TossPluginActivator started with configuration handler");
    }

    private void registerPaymentPluginApi(final BundleContext context, final PaymentPluginApi api) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, PaymentPluginApi.class, api, props);
    }
}
