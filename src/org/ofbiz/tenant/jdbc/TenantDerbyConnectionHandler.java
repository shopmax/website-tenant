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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.tenant.jdbc.TenantJdbcConnectionHandler;
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
    private boolean exists = false;
    
    /**
     * Constructor
     * @param jdbcUri
     * @param sqlProcessor
     */
    public TenantDerbyConnectionHandler(GenericValue tenantDataSource) {
        super(tenantDataSource);
    
        databaseDir = new File(System.getProperty("ofbiz.home") + File.separator + "runtime" + File.separator + "data"
                + File.separator + "derby" + File.separator + this.getDatabaseName());
        exists = databaseDir.exists();
    }
    /**
     * get JDBC Server name
     */
    @Override
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
        return exists;
    }
    
    @Override
    protected void doCreateDatabase(GenericHelperInfo helperInfo)
            throws GenericEntityException, SQLException {
        if (!isExist()) {
            Connection connection = DriverManager.getConnection(URI_PREFIX + getDatabaseName() + ";create=true");
            Statement statement = connection.createStatement();
            
            // Turn on built-in user
            
            // Setting and Confirming requireAuthentication
            statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" +
                "'derby.connection.requireAuthentication', 'true')");
            ResultSet rs = statement.executeQuery(
                "VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY(" +
                "'derby.connection.requireAuthentication')");
            rs.next();
            Debug.logInfo("Value of requireAuthentication is " + rs.getString(1), module);
            
            // Setting authentication scheme to Derby
            statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" +
                "'derby.authentication.provider', 'BUILTIN')");
            
            // Creating a user
            statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" +
                    "'derby.user." + getJdbcUsername() + "', '" + getJdbcPassword() + "')");
            
            // Setting default connection mode to no access (user authorization)
            statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" +
                "'derby.database.defaultConnectionMode', 'noAccess')");
            
            // Confirming default connection mode
            rs = statement.executeQuery (
                "VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY(" +
                "'derby.database.defaultConnectionMode')");
            rs.next();
            Debug.logInfo("Value of defaultConnectionMode is " + rs.getString(1), module);
            
            // Defining read-write users
            statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" +
                "'derby.database.fullAccessUsers', '" + getJdbcUsername() + "')");
            
            // Confirming full-access users
            rs = statement.executeQuery(
                "VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY(" +
                "'derby.database.fullAccessUsers')");
            rs.next();
            Debug.logInfo("Value of fullAccessUsers is " + rs.getString(1), module);
            
            // We would set the following property to TRUE only
            // when we were ready to deploy.
            statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" +
                "'derby.database.propertiesOnly', 'false')");
            statement.close();
            
            shutdown();
        }
    }

    @Override
    protected void doDeleteDatabase(GenericHelperInfo helperInfo) throws GenericEntityException, SQLException {
        shutdown();
        if (databaseDir.exists()) {
            Debug.logInfo("Delete database dirctory: " + databaseDir, module);
            TenantUtil.deleteDirectory(databaseDir);
        }
    }
    
    private void shutdown() {
        try {
            DriverManager.getConnection(URI_PREFIX + getDatabaseName() + ";shutdown=true");
        } catch (Exception e) {
            Debug.logWarning("Shutdown database: " + getDatabaseName(), module);
        }
    }
}
