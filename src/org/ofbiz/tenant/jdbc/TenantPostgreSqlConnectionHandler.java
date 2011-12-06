/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.tenant.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericDataSourceException;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.entity.jdbc.ConnectionFactory;
import org.ofbiz.entity.jdbc.SQLProcessor;

/**
 * Tenant PostreSQL connection handler
 * @author chatree
 *
 */
public class TenantPostgreSqlConnectionHandler extends TenantJdbcConnectionHandler {
    
    public final static String module = TenantPostgreSqlConnectionHandler.class.getName();
    
    public final static String URI_PREFIX = "jdbc:postgresql:";

    /**
     * Constructor
     * @param jdbcUri
     * @param sqlProcessor
     */
    public TenantPostgreSqlConnectionHandler(GenericValue tenantDataSource) {
        super(tenantDataSource);
    }
    
    /**
     * get database name
     */
    @Override
    public String getDatabaseName() {
        String databaseName = null;
        String jdbcUri = getJdbcUri();
        Pattern pattern = Pattern.compile(URI_PREFIX + "//127.0.0.1/(.*?)$");
        Matcher matcher = pattern.matcher(jdbcUri);
        if (matcher.find()) {
            databaseName = matcher.group(1);
        }
        return databaseName;
    }

    /**
     * delete database
     */
    @Override
    public int deleteDatabase(String databaseName) throws GenericDataSourceException {
        return 0;
    }

    /**
     * get SQL processor
     */
    @Override
    protected SQLProcessor getSQLProcessor() throws GenericEntityException, SQLException {
        SQLProcessor sqlProcessor = null;
        Delegator delegator = tenantDataSource.getDelegator();
        GenericHelperInfo helperInfo = delegator.getGroupHelperInfo(this.getEntityGroupName());
        try {
            Connection connection = ConnectionFactory.getConnection(this.getJdbcUri(), this.getJdbcUsername(), this.getJdbcPassword());
            sqlProcessor = new SQLProcessor(helperInfo, connection);
        } catch (Exception e) {
            // create new database
            Connection connection = ConnectionFactory.getConnection(helperInfo);
            sqlProcessor = new SQLProcessor(helperInfo, connection);
            sqlProcessor.executeUpdate("CREATE DATABASE \"" + this.getDatabaseName() + "\"");
            connection.close();
            connection = ConnectionFactory.getConnection(this.getJdbcUri(), this.getJdbcUsername(), this.getJdbcPassword());
            sqlProcessor = new SQLProcessor(helperInfo, connection);
        }
        return sqlProcessor;
    }
}
