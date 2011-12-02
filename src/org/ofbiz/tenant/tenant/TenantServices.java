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
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericDataSourceException;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.DelegatorInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.datasource.GenericHelper;
import org.ofbiz.entity.datasource.GenericHelperFactory;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.entity.jdbc.ConnectionFactory;
import org.ofbiz.entity.jdbc.SQLProcessor;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Element;

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
            GenericValue tenantDataSource = delegator.findOne("TenantDataSource", UtilMisc.toMap("tenantId", tenantId, "entityGroupName", entityGroupName), false);
            if (UtilValidate.isNotEmpty(tenantDataSource)) {
               DelegatorInfo delegatorInfo = EntityConfigUtil.getDelegatorInfo(delegator.getDelegatorBaseName());
               String dataResourceName =  delegatorInfo.groupMap.get(entityGroupName);
               DatasourceInfo dataSourceInfo = EntityConfigUtil.getDatasourceInfo(dataResourceName);
               Element inlineJdbcElement = dataSourceInfo.inlineJdbcElement;
                String connectionUrl = tenantDataSource.getString("jdbcUri");
                String userName = tenantDataSource.getString("jdbcUsername");
                String password = tenantDataSource.getString("jdbcPassword");
                String driverName = inlineJdbcElement.getAttribute("jdbc-driver");
                String databaseName = tenantId;
                String databaseOlapName = databaseName + "Olap";
                Properties props = null;
                GenericHelperInfo helperInfo = delegator.getGroupHelperInfo(entityGroupName);
                GenericHelper helper = GenericHelperFactory.getHelper(helperInfo);
                //Connection connection = ConnectionFactory.getConnection(driverName, connectionUrl, props, userName, password);
                Connection connection = ConnectionFactory.getConnection(helperInfo);
                SQLProcessor sqlProcessor = new SQLProcessor(helperInfo, connection);
                sqlProcessor.executeUpdate("CREATE DATABASE \"" + databaseName + "\"");
                sqlProcessor.executeUpdate("CREATE DATABASE \"" + databaseOlapName + "\"");
            }
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
}
