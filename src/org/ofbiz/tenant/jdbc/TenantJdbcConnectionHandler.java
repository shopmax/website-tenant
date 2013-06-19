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

import org.apache.commons.dbcp.PoolableConnection;
import org.apache.commons.dbcp.managed.TransactionContext;
import org.apache.commons.dbcp.managed.TransactionRegistry;
import org.apache.commons.dbcp.managed.XAConnectionFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.config.model.Datasource;
import org.ofbiz.entity.connection.DBCPConnectionFactory;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.entity.jdbc.ConnectionFactory;
import org.ofbiz.entity.util.EntityUtilProperties;

/**
 * Tenant JDBC connection handler
 * @author chatree
 *
 */
public abstract class TenantJdbcConnectionHandler {

    public final static String module = TenantJdbcConnectionHandler.class.getName();
    
    protected GenericValue tenantDataSource = null;
    protected String superUsername = null;
    protected String superPassword = null;
    
    /**
     * Constructor
     * @param jdbcUri
     * @param sqlProcessor
     */
    public TenantJdbcConnectionHandler(GenericValue tenantDataSource) {
        Delegator delegator = tenantDataSource.getDelegator();
        this.tenantDataSource = tenantDataSource;
        this.superUsername = EntityUtilProperties.getPropertyValue("tenant.properties", "superUsername", "postgres", delegator);
        this.superPassword = EntityUtilProperties.getPropertyValue("tenant.properties", "superPassword", "postgres", delegator);
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
     * get super username
     * @return
     */
    public String getSuperUsername() {
        return superUsername;
    }
    
    /**
     * get super password
     * @return
     */
    public String getSuperPassword() {
        return superPassword;
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
    
    public void createDatabase() throws GenericEntityException, SQLException {
        Delegator delegator = tenantDataSource.getDelegator();
        GenericHelperInfo helperInfo = delegator.getGroupHelperInfo(this.getEntityGroupName());
        helperInfo.setTenantId(this.getTenantId());
        Datasource datasource = EntityConfigUtil.getDatasource(helperInfo.getHelperBaseName());
        datasource.inlineJdbc.setJdbcUri(this.getJdbcUri());
        doCreateDatabase(helperInfo);
    }
    
    /**
     * delete database
     * @return
     */
    public void deleteDatabase() throws GenericEntityException, SQLException {
        Delegator delegator = tenantDataSource.getDelegator();
        GenericHelperInfo helperInfo = delegator.getGroupHelperInfo(this.getEntityGroupName());
        helperInfo.setTenantId(this.getTenantId());
        Datasource datasource = EntityConfigUtil.getDatasource(helperInfo.getHelperBaseName());
        datasource.inlineJdbc.setJdbcUri(this.getJdbcUri());
        
        // get pool and shared connection
        DBCPConnectionFactory managedConnectionFactory = (DBCPConnectionFactory) ConnectionFactory.getManagedConnectionFactory();
        GenericObjectPool pool = managedConnectionFactory.getGenericObjectPool(helperInfo);
        XAConnectionFactory xacf = managedConnectionFactory.getXAConnectionFactory(helperInfo);

        // return shared connection
        if (UtilValidate.isNotEmpty(xacf)) {
            TransactionRegistry transactionRegistry = xacf.getTransactionRegistry();
            TransactionContext transactionContext = transactionRegistry.getActiveTransactionContext();
            if (UtilValidate.isNotEmpty(transactionContext)) {
                PoolableConnection sharedConnection = (PoolableConnection) transactionContext.getSharedConnection();
                
                try {
                    pool.returnObject(sharedConnection);
                    pool.clear();
                } catch (Exception e) {
                    Debug.logError(e, module);
                }
            }
        }
        
        // do delete database
        doDeleteDatabase(helperInfo);
        
        // remove delegator
        String tenantDelegatorName = delegator.getDelegatorBaseName() + "#" + this.getTenantId();
        DelegatorFactory.removeDelegator(tenantDelegatorName);
        
        // remove connection
        managedConnectionFactory.removeConnection(helperInfo);
    }
    
    public abstract boolean isExist() ;
    protected abstract void doCreateDatabase(GenericHelperInfo helperInfo) throws GenericEntityException, SQLException;
    protected abstract void doDeleteDatabase(GenericHelperInfo helperInfo) throws GenericEntityException, SQLException;
    
    protected abstract String getJdbcServerName();
}
