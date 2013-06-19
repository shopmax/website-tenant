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
package org.ofbiz.tenant.util;

import java.io.File;
import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import javolution.util.FastList;

import org.apache.commons.dbcp.PoolableConnection;
import org.apache.commons.dbcp.managed.ManagedConnection;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.config.model.Datasource;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.entity.jdbc.ConnectionFactory;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * Tenant Utility
 * @author chatree
 *
 */
public class TenantUtil {

    public final static String module = TenantUtil.class.getName();
    
    /**
     * delete directory
     * @param directory
     * @return
     */
    public static boolean deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            String[] children = directory.list();
            for (int i=0; i<children.length; i++) {
                boolean success = TenantUtil.deleteDirectory(new File(directory, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return directory.delete();
    }
    
    /**
     * is active
     * @param tenantId
     * @param request
     * @return
     */
    public static boolean isActive(String tenantId, HttpServletRequest request) {
        String tId = (String) request.getAttribute("tenantId");
        if (tenantId.equals(tId)) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * is connection available
     * @param tenantId
     * @param delegator
     * @return
     */
    public static boolean isConnectionAvailable(String tenantId, Delegator delegator) {
        try {
            List<GenericValue> tenantDataSources = delegator.findByAnd("TenantDataSource", UtilMisc.toMap("tenantId", tenantId), null, false);
            for (GenericValue tenantDataSource : tenantDataSources) {
                String entityGroupName = tenantDataSource.getString("entityGroupName");
                String jdbcUri = tenantDataSource.getString("jdbcUri");
                String jdbcUsername = tenantDataSource.getString("jdbcUsername");
                String jdbcPassword = tenantDataSource.getString("jdbcPassword");
                
                GenericHelperInfo helperInfo = delegator.getGroupHelperInfo(entityGroupName);
                Connection connection = ConnectionFactory.getConnection(jdbcUri, jdbcUsername, jdbcPassword);
                ManagedConnection managedConn = (ManagedConnection) ConnectionFactory.getConnection(helperInfo);
                PoolableConnection poolConn = (PoolableConnection) managedConn.getDelegate();
                Connection innermostDelegate = poolConn.getInnermostDelegate();
                if (UtilValidate.isNotEmpty(connection)) {
                    if (!innermostDelegate.getClass().getName().equals(connection.getClass().getName())) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            Debug.logWarning(e, module);
            return false;
        }
        return true;
    }
    
    /**
     * set service fields
     * @param serviceName
     * @param context
     * @param toContext
     * @param timeZone
     * @param locale
     * @param dispatcher
     * @return
     */
    public static Map<String, Object> setServiceFields(String serviceName, Map<String, Object> context, Map<String, Object> toContext, TimeZone timeZone, Locale locale, LocalDispatcher dispatcher) {
        ModelService modelService = null;
        List<Object> errorMessages = FastList.newInstance();
        try {
            modelService = dispatcher.getDispatchContext().getModelService(serviceName);
        } catch (GenericServiceException e) {
            String errMsg = "In set-service-fields could not get service definition for service name [" + serviceName + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        toContext.putAll(modelService.makeValid(context, "IN", true, errorMessages, timeZone, locale));
        return ServiceUtil.returnSuccess();
    }
    
    public static boolean isSameJdbcType(String tenantId, Delegator delegator) {
        boolean isSameJdbcType = true;
        try {
            List<GenericValue> tenantDataSources = delegator.findByAnd("TenantDataSource", UtilMisc.toMap("tenantId", tenantId), null, false);
            for (GenericValue tenantDataSource : tenantDataSources) {
                String entityGroupName = tenantDataSource.getString("entityGroupName");
                String jdbcUri = tenantDataSource.getString("jdbcUri");
                String jdbcType = jdbcUri.split(":")[1];
                
                GenericHelperInfo helperInfo = delegator.getGroupHelperInfo(entityGroupName);
                Datasource datasource = EntityConfigUtil.getDatasource(helperInfo.getHelperBaseName());
                String targetJdbcUri = datasource.inlineJdbc.getJdbcUri();
                String targetJdbcType = targetJdbcUri.split(":")[1];
                
                isSameJdbcType = jdbcType.equals(targetJdbcType);
                if (!isSameJdbcType) return isSameJdbcType;
            }
        } catch (Exception e) {
            Debug.logWarning(e, module);
            isSameJdbcType = false;
        }
        return isSameJdbcType;
    }
}
