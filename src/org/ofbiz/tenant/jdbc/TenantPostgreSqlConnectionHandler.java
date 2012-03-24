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
import org.ofbiz.base.util.UtilValidate;
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
                Connection connection = ConnectionFactory.getConnection(this.getJdbcUri(), this.getJdbcUsername(), this.getJdbcPassword());
                SQLProcessor sqlProcessor = new SQLProcessor(helperInfo, connection);
                sqlProcessor.close();
                connection.close();
            } catch (Exception e) {
                // create JDBC username
                /*
                Connection userConnection = ConnectionFactory.getConnection(this.getPostgresJdbcUri(), this.getSuperUsername() , this.getSuperPassword());
                Statement statement = userConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
                ResultSet resultSet = statement.executeQuery("SELECT * FROM pg_roles WHERE rolname='" + this.getJdbcUsername() + "';");
                if (!resultSet.next()) {
                    statement.executeUpdate("CREATE ROLE \"" + this.getJdbcUsername() + "\" PASSWORD '" + this.getJdbcPassword() +"' SUPERUSER CREATEDB CREATEROLE INHERIT LOGIN;");
                    statement.close();
                    userConnection.close();
                }
                */
                
                // create a new database
                Connection connection = ConnectionFactory.getConnection(this.getPostgresJdbcUri(), this.getJdbcUsername(), this.getJdbcPassword());
                SQLProcessor sqlProcessor = new SQLProcessor(helperInfo, connection);
                sqlProcessor.executeUpdate("CREATE DATABASE \"" + this.getDatabaseName() + "\"");
                sqlProcessor.close();
                connection.close();
            }
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }
    /**
     * get JDBC Server name
     */
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
    protected void doDeleteDatabase(String databaseName, GenericHelperInfo helperInfo) throws GenericEntityException, SQLException {
        Connection conn = ConnectionFactory.getConnection(this.getPostgresJdbcUri(), this.getJdbcUsername(), this.getJdbcPassword());
        SQLProcessor sqlProcessor = new SQLProcessor(helperInfo, conn);
        sqlProcessor.executeUpdate("DROP DATABASE \"" + this.getDatabaseName() + "\"");
        sqlProcessor.close();
        conn.close();
    }
    
    protected String getPostgresJdbcUri() {
        return "jdbc:postgresql://127.0.0.1/postgres";
    }
}
