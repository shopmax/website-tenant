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

import java.io.File;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.tenant.util.TenantUtil;

/**
 * Tenant Derby connection handler
 * @author chatree
 *
 */
public class TenantDerbyConnectionHandler extends TenantJdbcConnectionHandler {
    
    public final static String module = TenantDerbyConnectionHandler.class.getName();
    
    public final static String URI_PREFIX = "jdbc:derby:";
    
    private File databaseDir = null;
    
    /**
     * Constructor
     * @param jdbcUri
     * @param sqlProcessor
     */
    public TenantDerbyConnectionHandler(GenericValue tenantDataSource) {
        super(tenantDataSource);
    
        databaseDir = new File(System.getProperty("ofbiz.home") + File.separator + "runtime" + File.separator + "data"
                + File.separator + "derby" + File.separator + this.getDatabaseName());
    }
    /**
     * get JDBC Server name
     */
    public String getJdbcServerName() {
        return null;
    }
    /**
     * get database name
     */
    @Override
    public String getDatabaseName() {
        String databaseName = null;
        String jdbcUri = getJdbcUri();
        Pattern pattern = Pattern.compile(URI_PREFIX + "(.*?);");
        Matcher matcher = pattern.matcher(jdbcUri);
        if (matcher.find()) {
            databaseName = matcher.group(1);
        }
        return databaseName;
    }
    
    @Override
    public boolean isExist() {
        return databaseDir.exists();
    }
    
    @Override
    protected void doCreateDatabase(GenericHelperInfo helperInfo)
            throws GenericEntityException, SQLException {
        
    }

    @Override
    protected void doDeleteDatabase(GenericHelperInfo helperInfo) throws GenericEntityException, SQLException {
        if (databaseDir.exists()) {
            Debug.logInfo("Delete database dirctory: " + databaseDir, module);
            TenantUtil.deleteDirectory(databaseDir);
        }
    }
    
    @Override
    protected void doRestoreDatabase(String contentId)
            throws GenericEntityException, SQLException {
        
    }
}
