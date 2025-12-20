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

create table toss_responses (
  record_id bigserial
, kb_account_id varchar(36) not null
, kb_payment_id varchar(36) not null
, kb_payment_transaction_id varchar(36) not null
, transaction_type varchar(32) not null
, amount numeric(15,9)
, currency varchar(3)
, payment_key varchar(255) default null
, order_id varchar(255) default null
, toss_payment_status varchar(50) default null
, toss_method varchar(50) default null
, toss_receipt_url varchar(255) default null
, additional_data text default null
, created_date timestamp not null
, kb_tenant_id varchar(36) not null
, primary key(record_id)
);
create index toss_responses_kb_payment_id on toss_responses(kb_payment_id);
create index toss_responses_kb_payment_transaction_id on toss_responses(kb_payment_transaction_id);
create index toss_responses_payment_key on toss_responses(payment_key);
create index toss_responses_kb_tenant_id on toss_responses(kb_tenant_id);

create table toss_payment_methods (
  record_id bigserial
, kb_account_id varchar(36) not null
, kb_payment_method_id varchar(36) not null
, billing_key varchar(255) not null
, customer_key varchar(255) default null
, is_default smallint not null default 0
, is_deleted smallint not null default 0
, additional_data text default null
, created_date timestamp not null
, updated_date timestamp not null
, kb_tenant_id varchar(36) not null
, primary key(record_id)
);
create unique index toss_payment_methods_kb_payment_method_id on toss_payment_methods(kb_payment_method_id);
create index toss_payment_methods_billing_key on toss_payment_methods(billing_key);
create index toss_payment_methods_kb_tenant_id on toss_payment_methods(kb_tenant_id);

create table toss_notifications (
  record_id bigserial
, kb_account_id varchar(36) default null
, kb_payment_id varchar(36) default null
, kb_payment_transaction_id varchar(36) default null
, event_type varchar(50) not null
, payment_key varchar(255) default null
, order_id varchar(255) default null
, notification_body text not null
, processed smallint not null default 0
, created_date timestamp not null
, kb_tenant_id varchar(36) not null
, primary key(record_id)
);
create index toss_notifications_payment_key on toss_notifications(payment_key);
create index toss_notifications_processed on toss_notifications(processed);
create index toss_notifications_kb_tenant_id on toss_notifications(kb_tenant_id);
