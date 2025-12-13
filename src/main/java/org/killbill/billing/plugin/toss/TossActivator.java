package org.killbill.billing.plugin.toss;

import java.util.Hashtable;

import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.osgi.framework.BundleContext;

public class TossActivator extends KillbillActivatorBase {

    public static final String PLUGIN_NAME = "killbill-toss";

    @Override
    public void start(final BundleContext context) {
        super.start(context);

        final TossPaymentPluginApi api = new TossPaymentPluginApi(killbillAPI, configProperties.getProperties());
        registerPaymentPluginApi(context, api);
    }

    private void registerPaymentPluginApi(final BundleContext context, final PaymentPluginApi api) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, PaymentPluginApi.class, api, props);
    }
}
