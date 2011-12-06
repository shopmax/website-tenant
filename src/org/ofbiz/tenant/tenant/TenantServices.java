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
package org.ofbiz.tenant.tenant;

import java.sql.SQLException;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericDataSourceException;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entityext.data.EntityDataLoadContainer;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.tenant.jdbc.TenantConnectionFactory;
import org.ofbiz.tenant.jdbc.TenantJdbcConnectionHandler;

/**
 * Tenant Services
 * @author chatree
 *
 */
public class TenantServices {

    public final static String module = TenantServices.class.getName();
    
    /**
     * create tenant database
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> createTenantDatabase(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String tenantId = (String) context.get("tenantId");
        String entityGroupName = (String) context.get("entityGroupName");
        
        try {
            TenantJdbcConnectionHandler connectionHandler = TenantConnectionFactory.getTenantJdbcConnectionHandler(tenantId, entityGroupName, delegator);
            String jdbcUri = connectionHandler.getJdbcUri();
            String databaseName = connectionHandler.getDatabaseName(jdbcUri);
            connectionHandler.createDatabase(databaseName);
        } catch (SQLException e) {
            String errMsg = "Could not create a database for tenant " + tenantId + " with entity group name " + entityGroupName + " : " + e.getMessage();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (GenericDataSourceException e) {
            String errMsg = "Could not create a database for tenant " + tenantId + " with entity group name " + entityGroupName + " : " + e.getMessage();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (GenericEntityException e) {
            String errMsg = "Could not create a database for tenant " + tenantId + " with entity group name " + entityGroupName + " : " + e.getMessage();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        return ServiceUtil.returnSuccess();
    }
    
    /**
     * install tenant database
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> installTenantDatabase(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String tenantId = (String) context.get("tenantId");
        String entityGroupName = (String) context.get("entityGroupName");
        String reader = (String) context.get("reader");
        try {
            String[] args = new String[3];
            args[0] = "-reader=" + reader;
            args[1] = "-delegator=" + delegator.getDelegatorBaseName() + "#" + tenantId;
            args[2] = "-group=" + entityGroupName;
            String configFile = FileUtil.getFile("component://base/config/install-containers.xml").getAbsolutePath();
            EntityDataLoadContainer entityDataLoadContainer = new EntityDataLoadContainer();
            entityDataLoadContainer.init(args, configFile);
            entityDataLoadContainer.start();
        } catch (Exception e) {
            String errMsg = "Could not install a database for tenant " + tenantId + " with entity group name " + entityGroupName + " : " + e.getMessage();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        return ServiceUtil.returnSuccess();
    }
    
    /**
     * delete tenant database
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> deleteTenantDatabase(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String tenantId = (String) context.get("tenantId");
        String entityGroupName = (String) context.get("entityGroupName");
        
        try {
            TenantJdbcConnectionHandler connectionHandler = TenantConnectionFactory.getTenantJdbcConnectionHandler(tenantId, entityGroupName, delegator);
            String jdbcUri = connectionHandler.getJdbcUri();
            String databaseName = connectionHandler.getDatabaseName(jdbcUri);
            connectionHandler.deleteDatabase(databaseName);
        } catch (SQLException e) {
            String errMsg = "Could not delete a database for tenant " + tenantId + " with entity group name " + entityGroupName + " : " + e.getMessage();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (GenericDataSourceException e) {
            String errMsg = "Could not delete a database for tenant " + tenantId + " with entity group name " + entityGroupName + " : " + e.getMessage();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (GenericEntityException e) {
            String errMsg = "Could not delete a database for tenant " + tenantId + " with entity group name " + entityGroupName + " : " + e.getMessage();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        return ServiceUtil.returnSuccess();
    }
}
