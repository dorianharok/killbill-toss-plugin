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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.stream.Collectors;

import org.killbill.billing.plugin.toss.dao.TossDao;
import org.killbill.commons.embeddeddb.h2.H2EmbeddedDB;

public class EmbeddedDbHelper {

    private static final String DDL_FILE_NAME = "ddl.sql";

    private static final EmbeddedDbHelper INSTANCE = new EmbeddedDbHelper();
    private H2EmbeddedDB embeddedDB;

    public static EmbeddedDbHelper instance() {
        return INSTANCE;
    }

    public void startDb() throws Exception {
        System.setProperty("org.jooq.no-logo", "true");

        embeddedDB = new H2EmbeddedDB("toss", "toss", "toss;MODE=MYSQL");
        embeddedDB.initialize();
        embeddedDB.start();

        final String ddl = readResource(DDL_FILE_NAME);
        embeddedDB.executeScript(ddl);
        embeddedDB.refreshTableNames();
    }

    public TossDao getTossDao() throws IOException, SQLException {
        return new TossDao(embeddedDB.getDataSource());
    }

    public void resetDB() throws Exception {
        embeddedDB.cleanupAllTables();
    }

    public void stopDB() throws Exception {
        embeddedDB.stop();
    }

    private String readResource(final String resourceName) throws IOException {
        try (final InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}
