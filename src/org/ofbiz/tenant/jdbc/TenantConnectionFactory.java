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
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * Tenant Connection Factory
 * @author chatree
 *
 */
public class TenantConnectionFactory {

    public final static String module = TenantConnectionFactory.class.getName();
    
    protected static Map<String, TenantJdbcConnectionHandler> connectionHandlers = FastMap.newInstance();
    
    /**
     * get tenant JDBC connection handler
     * @param tenantId
     * @param entityGroupName
     * @param delegator
     * @return
     * @throws GenericEntityException
     * @throws SQLException
     */
    public static TenantJdbcConnectionHandler getTenantJdbcConnectionHandler(String tenantId, String entityGroupName, Delegator delegator) throws GenericEntityException, SQLException {
        TenantJdbcConnectionHandler connectionHandler = null;
        GenericValue tenantDataSource = delegator.findOne("TenantDataSource", UtilMisc.toMap("tenantId", tenantId, "entityGroupName", entityGroupName), false);
        if (UtilValidate.isNotEmpty(tenantDataSource)) {
            Debug.logInfo("Create JDBC connection handler for tenant: " + tenantId + " with entity group: " + entityGroupName, module);
            String jdbcUri = tenantDataSource.getString("jdbcUri");
            
            connectionHandler = connectionHandlers.get(jdbcUri);
            if (UtilValidate.isEmpty(connectionHandler)) {
                try {
                    if (jdbcUri.startsWith(TenantDerbyConnectionHandler.URI_PREFIX)) { // Derby
                        Debug.logInfo("Create Derby connection handler", module);
                        connectionHandler = new TenantDerbyConnectionHandler(tenantDataSource);
                    } else if (jdbcUri.startsWith(TenantPostgreSqlConnectionHandler.URI_PREFIX)) { // PostgreSQL
                        Debug.logInfo("Create PostgreSQL connection handler", module);
                        connectionHandler = new TenantPostgreSqlConnectionHandler(tenantDataSource);
                    } else {
                        throw new GenericEntityException("Could not find a JDBC connection handler for: " + jdbcUri);
                    }
                } catch (Exception e) {
                    String errMsg = "Could not create a tenant connection handler for " + tenantId + " with entity group name " + entityGroupName + " : " + e.getMessage();
                    Debug.logError(e, errMsg, module);
                    throw new GenericEntityException(errMsg);
                }
            } else {
                return connectionHandler;
            }
        }
        return connectionHandler;
    }
}
