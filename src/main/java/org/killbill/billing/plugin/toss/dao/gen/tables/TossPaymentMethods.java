package org.killbill.billing.plugin.toss.dao.gen.tables;

import org.jooq.impl.TableImpl;
import org.killbill.billing.plugin.toss.dao.gen.tables.records.TossPaymentMethodsRecord;

public class TossPaymentMethods extends TableImpl<TossPaymentMethodsRecord> {
    public TossPaymentMethods() {
        super(org.jooq.impl.DSL.name("toss_payment_methods"));
    }
}
