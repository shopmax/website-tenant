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

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericDataSourceException;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.entity.jdbc.ConnectionFactory;
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
     * install tenant databases
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> installTenantDatabases(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String tenantId = (String) context.get("tenantId");
        String reader = (String) context.get("reader");
        
        List<String> errMsgs = FastList.newInstance();
        try {
            // fist make sure if connection handlers are exist
            List<GenericValue> tenantDataSources = delegator.findByAnd("TenantDataSource", UtilMisc.toMap("tenantId", tenantId));
            for (GenericValue tenantDataSource : tenantDataSources) {
                String entityGroupName = tenantDataSource.getString("entityGroupName");
                TenantJdbcConnectionHandler connectionHandler = TenantConnectionFactory.getTenantJdbcConnectionHandler(tenantId, entityGroupName, delegator);
            }
            
            String delegatorName = delegator.getDelegatorBaseName() + "#" + tenantId;
            String[] args = new String[2];
            args[0] = "-reader=" + reader;
            args[1] = "-delegator=" + delegatorName;
            String configFile = FileUtil.getFile("component://base/config/install-containers.xml").getAbsolutePath();
            
            // load data
            EntityDataLoadContainer entityDataLoadContainer = new EntityDataLoadContainer();
            entityDataLoadContainer.init(args, configFile);
            entityDataLoadContainer.start();
            
            // close connections
            Delegator tenantDelegator = DelegatorFactory.getDelegator(delegatorName);
            for (GenericValue tenantDataSource : tenantDataSources) {
                String entityGroupName = tenantDataSource.getString("entityGroupName");
                try {
                    GenericHelperInfo helperInfo = tenantDelegator.getGroupHelperInfo(entityGroupName);
                    Connection connection = ConnectionFactory.getConnection(helperInfo);
                    connection.close();
                } catch (Exception e) {
                    String errMsg = "Could not install a database for tenant " + tenantId + " with entity group name : " + entityGroupName + " : " + e.getMessage();
                    Debug.logError(e, errMsg, module);
                    errMsgs.add(errMsg);
                }
            }
            
            if (UtilValidate.isNotEmpty(errMsgs)) {
                return ServiceUtil.returnError(errMsgs);
            }
        } catch (Exception e) {
            String errMsg = "Could not install databases for tenant " + tenantId + " : " + e.getMessage();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        return ServiceUtil.returnSuccess();
    }
    
    /**
     * delete tenant databases
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> deleteTenantDatabases(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String tenantId = (String) context.get("tenantId");
        
        try {
            List<GenericValue> tenantDataSources = delegator.findByAnd("TenantDataSource", UtilMisc.toMap("tenantId", tenantId));
            for (GenericValue tenantDataSource : tenantDataSources) {
                String entityGroupName = tenantDataSource.getString("entityGroupName");
                try {
                    TenantJdbcConnectionHandler connectionHandler = TenantConnectionFactory.getTenantJdbcConnectionHandler(tenantId, entityGroupName, delegator);
                    String databaseName = connectionHandler.getDatabaseName();
                    connectionHandler.deleteDatabase(databaseName);
                } catch (Exception e) {
                    String errMsg = "Could not delete a database for tenant " + tenantId + " with entity group name : " + entityGroupName + " : " + e.getMessage();
                    Debug.logError(e, errMsg, module);
                }
            }
            
            List<String> errMsgs = FastList.newInstance();
            if (UtilValidate.isNotEmpty(errMsgs)) {
                return ServiceUtil.returnError(errMsgs);
            }
        } catch (GenericDataSourceException e) {
            String errMsg = "Could not delete databases for tenant " + tenantId + " : " + e.getMessage();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (GenericEntityException e) {
            String errMsg = "Could not delete databases for tenant " + tenantId + " : " + e.getMessage();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        return ServiceUtil.returnSuccess();
    }
}
