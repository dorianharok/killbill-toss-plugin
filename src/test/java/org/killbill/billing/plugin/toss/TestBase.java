/*
 * Copyright 2024 The Billing Project, LLC
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

package org.killbill.billing.plugin.toss;

import java.util.UUID;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.toss.api.TossPaymentPluginApi;
import org.killbill.billing.plugin.toss.client.TossClient;
import org.killbill.billing.plugin.toss.core.TossActivator;
import org.killbill.billing.plugin.toss.core.TossConfigProperties;
import org.killbill.billing.plugin.toss.core.TossConfigurationHandler;
import org.killbill.billing.plugin.toss.dao.TossDao;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.clock.ClockMock;
import org.mockito.Mockito;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

public abstract class TestBase {

    public static final Currency DEFAULT_CURRENCY = Currency.KRW;
    public static final String DEFAULT_COUNTRY = "KR";

    protected ClockMock clock;
    protected CallContext context;
    protected Account account;
    protected TossPaymentPluginApi tossPaymentPluginApi;
    protected OSGIKillbillAPI killbillApi;
    protected TossConfigurationHandler tossConfigurationHandler;
    protected TossDao dao;
    protected TossClient tossClient;

    @BeforeMethod(groups = {"slow"})
    public void setUp() throws Exception {
        EmbeddedDbHelper.instance().resetDB();
        dao = EmbeddedDbHelper.instance().getTossDao();

        clock = new ClockMock();

        context = Mockito.mock(CallContext.class);
        Mockito.when(context.getTenantId()).thenReturn(UUID.randomUUID());

        account = TestUtils.buildAccount(DEFAULT_CURRENCY, DEFAULT_COUNTRY);
        Mockito.when(account.getEmail()).thenReturn(UUID.randomUUID() + "@example.com");
        killbillApi = TestUtils.buildOSGIKillbillAPI(account);

        TestUtils.buildPaymentMethod(account.getId(), account.getPaymentMethodId(), TossActivator.PLUGIN_NAME, killbillApi);

        tossConfigurationHandler = new TossConfigurationHandler(TossActivator.PLUGIN_NAME, killbillApi);

        // Mock TossClient by default - subclasses can override
        tossClient = Mockito.mock(TossClient.class);

        final OSGIConfigPropertiesService configPropertiesService = Mockito.mock(OSGIConfigPropertiesService.class);
        tossPaymentPluginApi = new TossPaymentPluginApi(killbillApi,
                                                        configPropertiesService,
                                                        clock,
                                                        dao,
                                                        tossConfigurationHandler,
                                                        tossClient);

        TestUtils.updateOSGIKillbillAPI(killbillApi, tossPaymentPluginApi);

        // Set default config with test secret key
        final java.util.Properties props = new java.util.Properties();
        props.setProperty("org.killbill.billing.plugin.toss.secret_key", "test_sk_12345678");
        final TossConfigProperties tossConfigProperties = new TossConfigProperties(props);
        tossConfigurationHandler.setDefaultConfigurable(tossConfigProperties);
    }

    @BeforeSuite(groups = {"slow"})
    public void setUpBeforeSuite() throws Exception {
        EmbeddedDbHelper.instance().startDb();
    }

    @AfterSuite(groups = {"slow"})
    public void tearDownAfterSuite() throws Exception {
        EmbeddedDbHelper.instance().stopDB();
    }
}
