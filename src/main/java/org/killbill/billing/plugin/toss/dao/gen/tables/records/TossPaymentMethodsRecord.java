package org.killbill.billing.plugin.toss.dao.gen.tables.records;

import org.jooq.impl.UpdatableRecordImpl;

public class TossPaymentMethodsRecord extends UpdatableRecordImpl<TossPaymentMethodsRecord> {
    public TossPaymentMethodsRecord() {
        super(new org.killbill.billing.plugin.toss.dao.gen.tables.TossPaymentMethods());
    }
}
