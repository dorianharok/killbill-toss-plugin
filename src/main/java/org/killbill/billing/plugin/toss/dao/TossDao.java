package org.killbill.billing.plugin.toss.dao;

import java.sql.SQLException;
import javax.sql.DataSource;

import org.killbill.billing.plugin.dao.payment.PluginPaymentDao;
import org.killbill.billing.plugin.toss.dao.gen.tables.TossPaymentMethods;
import org.killbill.billing.plugin.toss.dao.gen.tables.TossResponses;
import org.killbill.billing.plugin.toss.dao.gen.tables.records.TossPaymentMethodsRecord;
import org.killbill.billing.plugin.toss.dao.gen.tables.records.TossResponsesRecord;

public class TossDao extends PluginPaymentDao<TossResponsesRecord, TossResponses, TossPaymentMethodsRecord, TossPaymentMethods> {

    public TossDao(final DataSource dataSource) throws SQLException {
        super(new TossResponses(), new TossPaymentMethods(), dataSource);
    }
}
