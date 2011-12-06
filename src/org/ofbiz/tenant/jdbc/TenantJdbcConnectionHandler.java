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

import org.ofbiz.entity.GenericDataSourceException;
import org.ofbiz.entity.jdbc.SQLProcessor;

/**
 * Tenant JDBC connection handler
 * @author chatree
 *
 */
public abstract class TenantJdbcConnectionHandler {
    
    protected String jdbcUri = null;
    protected SQLProcessor sqlProcessor = null;
    
    /**
     * Constructor
     * @param jdbcUri
     * @param sqlProcessor
     */
    public TenantJdbcConnectionHandler(String jdbcUri, SQLProcessor sqlProcessor) {
        this.jdbcUri = jdbcUri;
        this.sqlProcessor = sqlProcessor;
    }
    
    /**
     * get Jdbc Uri
     * @return
     */
    public String getJdbcUri() {
        return jdbcUri;
    }
    
    /**
     * get database name
     * @param jdbcUri
     * @return
     */
    public abstract String getDatabaseName(String jdbcUri);

    /**
     * create database
     * @return
     */
    public abstract int createDatabase(String databaseName) throws GenericDataSourceException;
    
    /**
     * delete database
     * @return
     */
    public abstract int deleteDatabase(String databaseName) throws GenericDataSourceException;
}
