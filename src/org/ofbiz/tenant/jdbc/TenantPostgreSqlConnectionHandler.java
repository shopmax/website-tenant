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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ofbiz.entity.GenericDataSourceException;
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
    public TenantPostgreSqlConnectionHandler(String jdbcUri, SQLProcessor sqlProcessor) {
        super(jdbcUri, sqlProcessor);
    }
    
    /**
     * get database name
     */
    @Override
    public String getDatabaseName(String jdbcUri) {
        String databaseName = null;
        Pattern pattern = Pattern.compile(URI_PREFIX + "//127.0.0.1/(.*?)$");
        Matcher matcher = pattern.matcher(jdbcUri);
        if (matcher.find()) {
            databaseName = matcher.group(1);
        }
        return databaseName;
    }

    /**
     * create database name
     */
    @Override
    public int createDatabase(String databaseName) throws GenericDataSourceException {
        return sqlProcessor.executeUpdate("CREATE DATABASE \"" + databaseName + "\"");
    }

    /**
     * delete database
     */
    @Override
    public int deleteDatabase(String databaseName) throws GenericDataSourceException {
        return 0;
    }

}
