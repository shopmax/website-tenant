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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.entity.jdbc.ConnectionFactory;
import org.ofbiz.entity.jdbc.SQLProcessor;
import org.ofbiz.tenant.jdbc.TenantJdbcConnectionHandler;

/**
 * Tenant PostreSQL connection handler
 * @author chatree
 *
 */
public class TenantPostgreSqlConnectionHandler extends TenantJdbcConnectionHandler {
    
    public final static String module = TenantPostgreSqlConnectionHandler.class.getName();
    
    public final static String URI_PREFIX = "jdbc:postgresql:";

    /**
     * Constructor
     * @param jdbcUri
     * @param sqlProcessor
     */
    public TenantPostgreSqlConnectionHandler(GenericValue tenantDataSource) {
        super(tenantDataSource);
        try {
            Delegator delegator = tenantDataSource.getDelegator();
            GenericHelperInfo helperInfo = delegator.getGroupHelperInfo(this.getEntityGroupName());
            try {
                Debug.logInfo("Get a connection of " + this.getJdbcUsername() + "@" + this.getJdbcUri() + " with " + this.getJdbcPassword(), module);
                Connection connection = ConnectionFactory.getConnection(this.getJdbcUri(), this.getJdbcUsername(), this.getJdbcPassword());
                SQLProcessor sqlProcessor = new SQLProcessor(helperInfo, connection);
                sqlProcessor.close();
                connection.close();
            } catch (Exception e) {
                Debug.logInfo("Database does not exist: " + this.getJdbcUri(), module);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }
    /**
     * get JDBC Server name
     */
    @Override
    public String getJdbcServerName() {
        String jdbcUri = getJdbcUri();
        String serverName;
        String []temp1,temp2;
        temp1 = jdbcUri.split("//");
        temp2 = temp1[1].split("/");
        
        serverName = temp2[0];
        return serverName;
    }
    
    /**
     * get database name
     */
    @Override
    public String getDatabaseName() {
        String databaseName = null;
        String jdbcUri = getJdbcUri();
        String jdbcServerName = getJdbcServerName();
        Pattern pattern = Pattern.compile(URI_PREFIX + "//" + jdbcServerName + "/(.*?)$");
        Matcher matcher = pattern.matcher(jdbcUri);
        if (matcher.find()) {
            databaseName = matcher.group(1);
        }
        return databaseName;
    }
    
    @Override
    public boolean isExist() {
        try {
            Delegator delegator = tenantDataSource.getDelegator();
            GenericHelperInfo helperInfo = delegator.getGroupHelperInfo(this.getEntityGroupName());
            Connection connection = ConnectionFactory.getConnection(this.getPostgresJdbcUri(), this.getSuperUsername(), this.getSuperPassword());
            SQLProcessor sqlProcessor = new SQLProcessor(helperInfo, connection);
            ResultSet rs = sqlProcessor.executeQuery("SELECT COUNT(*) FROM pg_database WHERE datname='" + this.getDatabaseName() + "' AND datistemplate = false");
            sqlProcessor.close();
            connection.close();
            int count = rs.getInt(0);
            return count > 0;
        } catch (Exception e) {
            Debug.logWarning(e, module);
            return false;
        }
    }
    
    @Override
    protected void doCreateDatabase(GenericHelperInfo helperInfo)
            throws GenericEntityException, SQLException {
        Debug.logInfo("Create database " + this.getJdbcUsername() + "@" + this.getJdbcUri() + " with " + this.getJdbcPassword(), module);
        // check if the user is not exist then create the user
        Debug.logInfo("Check a user[" + this.getJdbcUsername() + "] by " + this.getSuperUsername() + "@" + this.getPostgresJdbcUri() + " with " + this.getSuperPassword(), module);
        Connection superConnection = ConnectionFactory.getConnection(this.getPostgresJdbcUri(), this.getSuperUsername(), this.getSuperPassword());
        Statement statement = superConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        ResultSet resultSet = statement.executeQuery("SELECT * FROM pg_roles WHERE rolname='" + this.getJdbcUsername() + "';");
        if (!resultSet.next()) {
            // create JDBC username
            statement.executeUpdate("CREATE USER \"" + this.getJdbcUsername() + "\" WITH PASSWORD '" + this.getJdbcPassword() +"' LOGIN;");
            statement.close();
            superConnection.close();
        }
        
        // create a new database
        Connection connection = ConnectionFactory.getConnection(this.getPostgresJdbcUri(), this.getJdbcUsername(), this.getJdbcPassword());
        SQLProcessor sqlProcessor = new SQLProcessor(helperInfo, connection);
        sqlProcessor.executeUpdate("CREATE DATABASE \"" + this.getDatabaseName() + "\"");
        sqlProcessor.executeUpdate("ALTER DATABASE \"" + this.getDatabaseName() + "\" OWNER TO \"" + this.getJdbcUsername() + "\"");
        sqlProcessor.executeUpdate("GRANT ALL PRIVILEGES ON \"" + this.getDatabaseName() + "\" TO \"" + this.getJdbcUsername() + "\"");
        sqlProcessor.close();
        connection.close();
    }

    @Override
    protected void doDeleteDatabase(GenericHelperInfo helperInfo) throws GenericEntityException, SQLException {
        Connection conn = ConnectionFactory.getConnection(this.getPostgresJdbcUri(), this.getJdbcUsername(), this.getJdbcPassword());
        SQLProcessor sqlProcessor = new SQLProcessor(helperInfo, conn);
        sqlProcessor.executeUpdate("DROP DATABASE \"" + this.getDatabaseName() + "\"");
        sqlProcessor.close();
        conn.close();
    }
    
    protected String getPostgresJdbcUri() {
        return "jdbc:postgresql://" + this.getJdbcServerName() + "/postgres";
    }
}
