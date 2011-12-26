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

import java.sql.SQLException;

import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * Tenant JDBC connection handler
 * @author chatree
 *
 */
public abstract class TenantJdbcConnectionHandler {

    public final static String module = TenantJdbcConnectionHandler.class.getName();
    
    protected GenericValue tenantDataSource = null;
    
    /**
     * Constructor
     * @param jdbcUri
     * @param sqlProcessor
     */
    public TenantJdbcConnectionHandler(GenericValue tenantDataSource) {
        this.tenantDataSource = tenantDataSource;
    }
    
    /**
    * get tenant ID
    * @return
    */
    public String getTenantId() {
        return tenantDataSource.getString("tenantId");
    }
    
    /**
     * get Jdbc Uri
     * @return
     */
    public String getJdbcUri() {
        return tenantDataSource.getString("jdbcUri");
    }
    
    /**
     * get entity group name
     * @return
     */
    public String getEntityGroupName() {
        return tenantDataSource.getString("entityGroupName");
    }
    
    /**
     * get JDBC username
     * @return
     */
    public String getJdbcUsername() {
        return tenantDataSource.getString("jdbcUsername");
    }
    
    /**
     * get JDBC password
     * @return
     */
    public String getJdbcPassword() {
        return tenantDataSource.getString("jdbcPassword");
    }
    
    /**
     * get database name
     * @param jdbcUri
     * @return
     */
    public abstract String getDatabaseName();
    
    /**
     * delete database
     * @return
     */
    public abstract int deleteDatabase(String databaseName) throws GenericEntityException, SQLException;
}
