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

package org.killbill.billing.plugin.toss.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.payment.PluginPaymentMethodPlugin;
import org.killbill.billing.plugin.toss.dao.TossDao;
import org.killbill.billing.plugin.toss.dao.gen.tables.records.TossPaymentMethodsRecord;

public class TossPaymentMethodPlugin extends PluginPaymentMethodPlugin {

    public static final short TRUE = 1;
    public static final short FALSE = 0;

    public static TossPaymentMethodPlugin build(final TossPaymentMethodsRecord record) {
        final Map additionalData = TossDao.fromAdditionalData(record.getAdditionalData());
        final String billingKey = record.getBillingKey();

        return new TossPaymentMethodPlugin(
                UUID.fromString(record.getKbPaymentMethodId()),
                billingKey,
                record.getIsDefault() == TRUE,
                PluginProperties.buildPluginProperties(additionalData)
        );
    }

    public TossPaymentMethodPlugin(final UUID kbPaymentMethodId,
                                   final String externalPaymentMethodId,
                                   final boolean isDefault,
                                   final List<PluginProperty> properties) {
        super(kbPaymentMethodId,
              externalPaymentMethodId,
              isDefault,
              properties);
    }
}
