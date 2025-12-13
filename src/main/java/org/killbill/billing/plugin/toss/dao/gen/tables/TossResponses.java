package org.killbill.billing.plugin.toss.dao.gen.tables;

import org.jooq.impl.TableImpl;
import org.killbill.billing.plugin.toss.dao.gen.tables.records.TossResponsesRecord;

public class TossResponses extends TableImpl<TossResponsesRecord> {
    public TossResponses() {
        super(org.jooq.impl.DSL.name("toss_responses"));
    }
}
